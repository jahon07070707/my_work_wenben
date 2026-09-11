package com.textanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.textanalysis.entity.*;
import com.textanalysis.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final RestTemplate restTemplate;
    private final RawTextMapper rawTextMapper;
    private final AnalysisResultMapper analysisResultMapper;
    private final HotTopicMapper hotTopicMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    @Cacheable(value = "analysisCache", key = "#text", unless = "#result == null")
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeText(String text) {
        String url = mlServiceUrl + "/analyze";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of("text", text);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() == null) {
                throw new RuntimeException("ML 服务返回空结果");
            }
            return response.getBody();
        } catch (ResourceAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("文本分析失败: " + e.getMessage(), e);
        }
    }

    public AnalysisResult analyzeAndSave(String text, String title) {
        Map<String, Object> result = analyzeText(text);

        RawText rawText = new RawText();
        rawText.setTitle(title != null ? title : text.substring(0, Math.min(50, text.length())));
        rawText.setContent(text);
        rawText.setIsAnalyzed(1);
        rawTextMapper.insert(rawText);

        AnalysisResult ar = new AnalysisResult();
        ar.setTextId(rawText.getId());
        ar.setCategory((String) result.get("category"));
        ar.setCategoryConfidence(BigDecimal.valueOf((Double) result.get("category_confidence")));
        ar.setSentiment((String) result.get("sentiment"));
        ar.setSentimentConfidence(BigDecimal.valueOf((Double) result.get("sentiment_confidence")));

        List<String> keywords = (List<String>) result.get("keywords");
        ar.setKeywords(String.join(",", keywords));
        analysisResultMapper.insert(ar);

        updateHotTopics(keywords);
        return ar;
    }

    public int batchAnalyzeUnprocessed(int limit) {
        List<RawText> texts = rawTextMapper.findUnanalyzed(limit);
        int count = 0;
        for (RawText rt : texts) {
            try {
                Map<String, Object> result = analyzeText(rt.getContent());
                AnalysisResult ar = new AnalysisResult();
                ar.setTextId(rt.getId());
                ar.setCategory((String) result.get("category"));
                ar.setCategoryConfidence(BigDecimal.valueOf((Double) result.get("category_confidence")));
                ar.setSentiment((String) result.get("sentiment"));
                ar.setSentimentConfidence(BigDecimal.valueOf((Double) result.get("sentiment_confidence")));
                List<String> keywords = (List<String>) result.get("keywords");
                ar.setKeywords(String.join(",", keywords));
                analysisResultMapper.insert(ar);
                rawTextMapper.markAnalyzed(rt.getId());
                updateHotTopics(keywords);
                count++;
            } catch (Exception e) {
                log.error("分析失败 textId={}: {}", rt.getId(), e.getMessage());
            }
        }
        return count;
    }

    private void updateHotTopics(List<String> keywords) {
        LocalDate today = LocalDate.now();
        for (String kw : keywords) {
            HotTopic topic = new HotTopic();
            topic.setKeyword(kw);
            topic.setCount(1);
            topic.setStatDate(today);
            hotTopicMapper.upsert(topic);
        }
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTexts", rawTextMapper.countAll());
        stats.put("categoryDistribution", analysisResultMapper.countByCategory());
        stats.put("sentimentDistribution", analysisResultMapper.countBySentiment());
        stats.put("trendData", analysisResultMapper.countByDate(7));
        stats.put("hotTopics", hotTopicMapper.findTop(10));
        stats.put("wordCloud", analysisResultMapper.getWordCloudData(50));
        return stats;
    }

    public List<Map<String, Object>> getTrendData(String range) {
        return switch (range) {
            case "day" -> analysisResultMapper.countByHour();
            case "month" -> analysisResultMapper.countByDate(30);
            default -> analysisResultMapper.countByDate(7);
        };
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> batchAnalyzeTexts(List<String> texts, boolean save) {
        String url = mlServiceUrl + "/analyze/batch";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("texts", texts);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<List> response = restTemplate.postForEntity(url, entity, List.class);
        List<Map<String, Object>> results = response.getBody();
        if (results == null) return List.of();

        if (save) {
            for (int i = 0; i < texts.size() && i < results.size(); i++) {
                String text = texts.get(i);
                if (text == null || text.isBlank()) continue;
                try {
                    saveAnalysisResult(text, results.get(i));
                } catch (Exception e) {
                    log.warn("Batch save failed: {}", e.getMessage());
                }
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private void saveAnalysisResult(String text, Map<String, Object> result) {
        RawText rawText = new RawText();
        rawText.setTitle(text.substring(0, Math.min(50, text.length())));
        rawText.setContent(text);
        rawText.setIsAnalyzed(1);
        rawTextMapper.insert(rawText);

        AnalysisResult ar = new AnalysisResult();
        ar.setTextId(rawText.getId());
        ar.setCategory((String) result.get("category"));
        ar.setCategoryConfidence(toBigDecimal(result.get("category_confidence")));
        ar.setSentiment((String) result.get("sentiment"));
        ar.setSentimentConfidence(toBigDecimal(result.get("sentiment_confidence")));
        List<String> keywords = (List<String>) result.get("keywords");
        ar.setKeywords(String.join(",", keywords));
        analysisResultMapper.insert(ar);
        updateHotTopics(keywords);
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    public Map<String, Object> preprocessText(String text) {
        String url = mlServiceUrl + "/preprocess";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("text", text), headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> getModelMetrics() {
        try {
            String url = mlServiceUrl + "/metrics";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                return emptyMetrics("ML 服务返回空数据");
            }
            return body;
        } catch (ResourceAccessException e) {
            return emptyMetrics("Python ML 服务未启动，请先运行 python/start-ml.bat");
        } catch (Exception e) {
            return emptyMetrics("获取模型指标失败: " + e.getMessage());
        }
    }

    private Map<String, Object> emptyMetrics(String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("meta", null);
        m.put("metrics", null);
        m.put("message", message);
        return m;
    }

    public String exportCsv(String category, String sentiment) {
        List<AnalysisResult> list = analysisResultMapper.findAllForExport(category, sentiment);
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF"); // BOM for Excel UTF-8
        sb.append("标题,内容,分类,分类置信度,情感,情感置信度,关键词,分析时间\n");
        for (AnalysisResult ar : list) {
            sb.append(csvEscape(ar.getTitle())).append(',')
              .append(csvEscape(ar.getContent())).append(',')
              .append(csvEscape(ar.getCategory())).append(',')
              .append(ar.getCategoryConfidence()).append(',')
              .append(csvEscape(ar.getSentiment())).append(',')
              .append(ar.getSentimentConfidence()).append(',')
              .append(csvEscape(ar.getKeywords())).append(',')
              .append(ar.getAnalyzedAt()).append('\n');
        }
        return sb.toString();
    }

    private String csvEscape(String val) {
        if (val == null) return "";
        String s = val.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return "\"" + s + "\"";
        }
        return s;
    }

    public Map<String, Object> getResults(int page, int size, String category, String sentiment) {
        int offset = (page - 1) * size;
        List<AnalysisResult> list = analysisResultMapper.findWithText(offset, size, category, sentiment);
        int total = analysisResultMapper.countWithFilter(category, sentiment);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}
