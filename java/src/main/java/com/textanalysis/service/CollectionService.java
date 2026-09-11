package com.textanalysis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.textanalysis.entity.*;
import com.textanalysis.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionService {

    private final DataSourceMapper dataSourceMapper;
    private final CollectionTaskMapper collectionTaskMapper;
    private final RawTextMapper rawTextMapper;
    private final AnalysisService analysisService;
    private final WebSocketPushService pushService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${crawler.python-home:../python}")
    private String pythonHome;

    @Value("${crawler.script-path:../python/crawler/main.py}")
    private String crawlerScript;

    @Value("${crawler.python-path:python}")
    private String pythonPath;

    private Path resolvePythonHome() {
        return Paths.get(pythonHome).toAbsolutePath().normalize();
    }

    public List<DataSource> listSources() {
        return dataSourceMapper.findAll();
    }

    public CollectionTask startCollection(Long sourceId) {
        DataSource source = dataSourceMapper.findById(sourceId);
        if (source == null) {
            throw new RuntimeException("数据源不存在");
        }

        CollectionTask task = new CollectionTask();
        task.setSourceId(sourceId);
        task.setStatus("running");
        task.setStartedAt(LocalDateTime.now());
        collectionTaskMapper.insert(task);
        pushService.pushTaskProgress(task.getId(), "running", "开始采集: " + source.getName(), 10);

        runCollectionAsync(task, source);
        return task;
    }

    public CollectionTask startAllCollection() {
        CollectionTask task = new CollectionTask();
        task.setStatus("running");
        task.setStartedAt(LocalDateTime.now());
        collectionTaskMapper.insert(task);
        pushService.pushTaskProgress(task.getId(), "running", "开始一键采集全部数据源", 5);

        runAllCollectionAsync(task);
        return task;
    }

    @Async
    public void runCollectionAsync(CollectionTask task, DataSource source) {
        try {
            pushService.pushTaskProgress(task.getId(), "running", "正在采集数据...", 30);
            int count;
            if ("comment".equals(source.getSourceType()) || source.getUrl().endsWith(".csv")) {
                count = importLocalCsv(source, task.getId());
            } else {
                count = runCrawlerAndImport(task.getId());
            }

            pushService.pushTaskProgress(task.getId(), "running", "采集完成，正在分析 " + count + " 条", 60);
            task.setStatus("success");
            task.setTotalCount(count);
            task.setSuccessCount(count);
            task.setFinishedAt(LocalDateTime.now());
            collectionTaskMapper.update(task);

            analysisService.batchAnalyzeUnprocessed(count);
            alertService.checkAndGenerateAlerts();
            pushService.pushTaskProgress(task.getId(), "success", "采集并分析完成，共 " + count + " 条", 100);
        } catch (Exception e) {
            log.error("采集失败", e);
            task.setStatus("failed");
            task.setErrorMsg(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            collectionTaskMapper.update(task);
            pushService.pushTaskProgress(task.getId(), "failed", "采集失败: " + e.getMessage(), 100);
        }
    }

    @Async
    public void runAllCollectionAsync(CollectionTask task) {
        try {
            pushService.pushTaskProgress(task.getId(), "running", "正在执行爬虫...", 20);
            int count = runCrawlerAndImport(task.getId());
            pushService.pushTaskProgress(task.getId(), "running", "正在批量分析...", 70);
            task.setStatus("success");
            task.setTotalCount(count);
            task.setSuccessCount(count);
            task.setFinishedAt(LocalDateTime.now());
            collectionTaskMapper.update(task);
            analysisService.batchAnalyzeUnprocessed(count);
            alertService.checkAndGenerateAlerts();
            pushService.pushTaskProgress(task.getId(), "success", "全部完成，共 " + count + " 条", 100);
        } catch (Exception e) {
            task.setStatus("failed");
            task.setErrorMsg(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            collectionTaskMapper.update(task);
            pushService.pushTaskProgress(task.getId(), "failed", e.getMessage(), 100);
        }
    }

    private int runCrawlerAndImport(Long taskId) throws Exception {
        Path scriptPath = Paths.get(crawlerScript).toAbsolutePath().normalize();
        Path crawlerDir = scriptPath.getParent();

        ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath.toString());
        pb.directory(crawlerDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info("[Crawler] {}", line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("采集脚本执行失败: " + output);
        }

        Path rawDir = resolvePythonHome().resolve("data/raw");
        if (!Files.exists(rawDir)) {
            return 0;
        }

        Optional<Path> latest = Files.list(rawDir)
                .filter(p -> p.toString().endsWith(".json"))
                .max(Comparator.comparing(p -> {
                    try { return Files.getLastModifiedTime(p); } catch (IOException e) { return null; }
                }));

        if (latest.isEmpty()) return 0;
        return importJsonFile(latest.get(), taskId);
    }

    private int importJsonFile(Path jsonFile, Long taskId) throws IOException {
        List<Map<String, Object>> items = objectMapper.readValue(jsonFile.toFile(),
                new TypeReference<List<Map<String, Object>>>() {});
        int count = 0;
        for (Map<String, Object> item : items) {
            RawText rt = new RawText();
            rt.setTaskId(taskId);
            rt.setTitle((String) item.getOrDefault("title", ""));
            rt.setContent((String) item.get("content"));
            rt.setUrl((String) item.getOrDefault("url", ""));
            rt.setAuthor((String) item.getOrDefault("author", ""));
            rt.setIsAnalyzed(0);
            rawTextMapper.insert(rt);
            count++;
        }
        return count;
    }

    private int importLocalCsv(DataSource source, Long taskId) throws IOException {
        Path csvPath = Paths.get(source.getUrl());
        if (!csvPath.isAbsolute()) {
            csvPath = resolvePythonHome().resolve(source.getUrl()).normalize();
        }
        if (!Files.exists(csvPath)) {
            throw new RuntimeException("CSV文件不存在: " + csvPath);
        }

        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length < 1) continue;
                RawText rt = new RawText();
                rt.setSourceId(source.getId());
                rt.setTaskId(taskId);
                if (parts.length >= 3) {
                    rt.setContent(parts[0]);
                    rt.setTitle(parts[0].substring(0, Math.min(50, parts[0].length())));
                } else if (parts.length >= 2) {
                    rt.setTitle(parts[0]);
                    rt.setContent(parts[1]);
                } else {
                    rt.setContent(parts[0]);
                    rt.setTitle(parts[0].substring(0, Math.min(50, parts[0].length())));
                }
                rt.setIsAnalyzed(0);
                rawTextMapper.insert(rt);
                count++;
            }
        }
        return count;
    }

    public List<CollectionTask> recentTasks() {
        return collectionTaskMapper.findRecent(20);
    }
}