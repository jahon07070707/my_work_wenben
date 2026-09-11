package com.textanalysis.controller;

import com.textanalysis.common.Result;
import com.textanalysis.dto.AnalyzeRequest;
import com.textanalysis.entity.AnalysisResult;
import com.textanalysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/text")
    public Result<Map<String, Object>> analyze(@RequestBody AnalyzeRequest request) {
        if (request.getText() == null || request.getText().isBlank()) {
            return Result.fail("文本内容不能为空");
        }
        return Result.ok(analysisService.analyzeText(request.getText()));
    }

    @PostMapping("/preprocess")
    public Result<Map<String, Object>> preprocess(@RequestBody AnalyzeRequest request) {
        if (request.getText() == null || request.getText().isBlank()) {
            return Result.fail("文本内容不能为空");
        }
        return Result.ok(analysisService.preprocessText(request.getText()));
    }

    @PostMapping("/text/save")
    public Result<AnalysisResult> analyzeAndSave(@RequestBody AnalyzeRequest request) {
        AnalysisResult result = analysisService.analyzeAndSave(request.getText(), null);
        return Result.ok(result);
    }

    @PostMapping("/batch")
    public Result<Integer> batchAnalyze(@RequestParam(defaultValue = "50") int limit) {
        int count = analysisService.batchAnalyzeUnprocessed(limit);
        return Result.ok(count);
    }

    @PostMapping("/batch-texts")
    public Result<List<Map<String, Object>>> batchTexts(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> texts = (List<String>) body.get("texts");
        boolean save = Boolean.TRUE.equals(body.get("save"));
        if (texts == null || texts.isEmpty()) {
            return Result.fail("文本列表不能为空");
        }
        if (texts.size() > 100) {
            return Result.fail("单次最多分析100条文本");
        }
        return Result.ok(analysisService.batchAnalyzeTexts(texts, save));
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean save) throws Exception {
        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        List<String> texts = parseFile(file);
        if (texts.isEmpty()) {
            return Result.fail("文件中未解析到有效文本");
        }
        if (texts.size() > 100) {
            texts = texts.subList(0, 100);
        }
        List<Map<String, Object>> results = analysisService.batchAnalyzeTexts(texts, save);
        return Result.ok(Map.of(
                "total", texts.size(),
                "results", results
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sentiment) {
        String csv = analysisService.exportCsv(category, sentiment);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analysis_results.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        return Result.ok(analysisService.getModelMetrics());
    }

    @GetMapping("/results")
    public Result<Map<String, Object>> getResults(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sentiment) {
        return Result.ok(analysisService.getResults(page, size, category, sentiment));
    }

    private List<String> parseFile(MultipartFile file) throws Exception {
        List<String> texts = new ArrayList<>();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            if (filename.endsWith(".csv")) {
                String header = reader.readLine();
                boolean contentFirst = header != null && header.toLowerCase().contains("content");
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 3 && contentFirst) {
                        texts.add(parts[0].trim());
                    } else if (parts.length >= 2) {
                        texts.add(parts[parts.length - 1].trim());
                    } else if (parts.length == 1 && !parts[0].isBlank()) {
                        texts.add(parts[0].trim());
                    }
                }
            } else {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) texts.add(line);
                }
            }
        }
        return texts;
    }
}
