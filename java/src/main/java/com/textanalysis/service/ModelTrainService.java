package com.textanalysis.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelTrainService {

    private final RestTemplate restTemplate;
    private final WebSocketPushService pushService;

    @Value("${ml.python-home:../python}")
    private String pythonHome;

    @Value("${ml.python-path:python}")
    private String pythonPath;

    @Value("${ml.venv-python:../python/.venv/Scripts/python.exe}")
    private String venvPython;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    private final TrainStatus status = new TrainStatus();

    @Data
    public static class TrainStatus {
        private String state = "idle"; // idle, running, success, failed
        private int progress = 0;
        private String message = "等待训练";
        private List<String> logs = new ArrayList<>();
        private Map<String, Object> result;
    }

    public synchronized TrainStatus getStatus() {
        return status;
    }

    public synchronized void startTrain() {
        if ("running".equals(status.getState())) {
            throw new RuntimeException("模型正在训练中，请稍候");
        }
        status.setState("running");
        status.setProgress(5);
        status.setMessage("正在启动训练...");
        status.getLogs().clear();
        status.setResult(null);
        runTrainAsync();
    }

    @Async
    public void runTrainAsync() {
        try {
            Path home = Paths.get(pythonHome).toAbsolutePath().normalize();
            Path mlDir = home.resolve("ml");
            Path trainScript = mlDir.resolve("train.py");
            Path dataDir = home.resolve("data");
            Path modelDir = mlDir.resolve("saved_models");

            if (!Files.exists(trainScript)) {
                throw new RuntimeException("训练脚本不存在: " + trainScript);
            }

            String py = resolvePython(home);
            appendLog("Python: " + py);
            appendLog("开始训练模型，预计 1-3 分钟...");

            ProcessBuilder pb = new ProcessBuilder(
                    py, trainScript.toString(),
                    "--data-dir", dataDir.toString(),
                    "--model-dir", modelDir.toString()
            );
            pb.directory(mlDir.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog(line);
                    updateProgress(line);
                    log.info("[Train] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("训练脚本退出码: " + exitCode);
            }

            reloadMlModel();
            Map<String, Object> metrics = fetchMetrics();

            synchronized (status) {
                status.setState("success");
                status.setProgress(100);
                status.setMessage("训练完成，模型已自动加载");
                status.setResult(metrics);
            }
            pushService.pushTaskProgress(0L, "success", "模型训练完成", 100);
        } catch (Exception e) {
            log.error("Model training failed", e);
            synchronized (status) {
                status.setState("failed");
                status.setMessage("训练失败: " + e.getMessage());
                appendLog("ERROR: " + e.getMessage());
            }
            pushService.pushTaskProgress(0L, "failed", "模型训练失败: " + e.getMessage(), 100);
        }
    }

    private String resolvePython(Path home) {
        Path venvWin = home.resolve(".venv/Scripts/python.exe");
        if (Files.exists(venvWin)) {
            return venvWin.toString();
        }
        Path venvUnix = home.resolve(".venv/bin/python");
        if (Files.exists(venvUnix)) {
            return venvUnix.toString();
        }
        return pythonPath;
    }

    private void updateProgress(String line) {
        if (line.contains("Loading training") || line.contains("加载训练")) {
            status.setProgress(15);
        } else if (line.contains("Vectorizing") || line.contains("向量化")) {
            status.setProgress(25);
        } else if (line.contains("classification") || line.contains("分类模型")) {
            status.setProgress(40);
        } else if (line.contains("sentiment") || line.contains("情感")) {
            status.setProgress(70);
        } else if (line.contains("Epoch")) {
            int p = Math.min(status.getProgress() + 2, 90);
            status.setProgress(p);
        } else if (line.contains("saved") || line.contains("保存")) {
            status.setProgress(95);
        }
        status.setMessage(line.length() > 60 ? line.substring(0, 60) + "..." : line);
    }

    private synchronized void appendLog(String line) {
        status.getLogs().add(line);
        if (status.getLogs().size() > 100) {
            status.getLogs().remove(0);
        }
    }

    private void reloadMlModel() {
        try {
            restTemplate.postForEntity(mlServiceUrl + "/reload", null, Map.class);
            appendLog("ML 推理服务已重新加载模型");
        } catch (Exception e) {
            appendLog("警告: 模型重载失败，请手动重启 Python 服务 - " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchMetrics() {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(mlServiceUrl + "/metrics", Map.class);
            return resp.getBody();
        } catch (Exception e) {
            return Map.of("message", "训练完成，但获取指标失败: " + e.getMessage());
        }
    }
}
