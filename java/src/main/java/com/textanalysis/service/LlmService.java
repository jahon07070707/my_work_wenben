package com.textanalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.textanalysis.dto.ChatRequest;
import com.textanalysis.entity.HotTopic;
import com.textanalysis.entity.LlmReport;
import com.textanalysis.mapper.LlmReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final RestTemplate restTemplate;
    private final LlmReportMapper reportMapper;
    private final AnalysisService analysisService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.enabled:false}")
    private boolean llmEnabled;

    @Value("${llm.api-url:}")
    private String apiUrl;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:deepseek-chat}")
    private String model;

    public Map<String, Object> generateReport(String username) {
        Map<String, Object> stats = analysisService.getDashboardStats();
        String prompt = buildPrompt(stats);
        String content;
        String source;

        if (llmEnabled && apiKey != null && !apiKey.isBlank()) {
            try {
                content = callLlmApi(prompt);
                source = "llm";
            } catch (Exception e) {
                log.warn("LLM call failed, using rule-based fallback: {}", e.getMessage());
                content = generateRuleBasedReport(stats);
                source = "rule_fallback";
            }
        } else {
            content = generateRuleBasedReport(stats);
            source = "rule_engine";
        }

        LlmReport report = new LlmReport();
        report.setReportType("daily");
        report.setContent(content);
        report.setCreatedBy(username);
        reportMapper.insert(report);

        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("source", source);
        result.put("llmEnabled", llmEnabled);
        result.put("reportId", report.getId());
        return result;
    }

    public List<LlmReport> getHistory(int limit) {
        return reportMapper.findRecent(limit);
    }

    public Map<String, Object> getConfig() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("enabled", isLlmReady());
        cfg.put("model", model);
        return cfg;
    }

    public Map<String, Object> chat(List<ChatRequest.ChatMessage> messages) {
        if (!isLlmReady()) {
            throw new IllegalStateException("请先在 application.yml 中设置 llm.enabled=true 并配置 llm.api-key");
        }
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("消息不能为空");
        }

        List<Map<String, String>> payload = new ArrayList<>();
        payload.add(Map.of("role", "system", "content",
                "你是智能文本分析系统的AI助手，擅长中文文本分类、情感分析、舆情解读与数据分析。" +
                        "回答应简洁清晰，必要时可分点说明。若用户询问系统功能，可介绍文本分析、批量分析、舆情预警等模块。"));

        for (ChatRequest.ChatMessage msg : messages) {
            if (msg.getRole() == null || msg.getContent() == null || msg.getContent().isBlank()) {
                continue;
            }
            String role = msg.getRole();
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            payload.add(Map.of("role", role, "content", msg.getContent().trim()));
        }
        if (payload.size() <= 1) {
            throw new IllegalArgumentException("请至少发送一条有效消息");
        }

        try {
            String reply = callLlmApiWithMessages(payload);
            Map<String, Object> result = new HashMap<>();
            result.put("content", reply);
            result.put("model", model);
            return result;
        } catch (Exception e) {
            log.error("LLM chat failed", e);
            throw new IllegalStateException("大模型调用失败：" + e.getMessage());
        }
    }

    private boolean isLlmReady() {
        return llmEnabled && apiKey != null && !apiKey.isBlank();
    }

    private String buildPrompt(Map<String, Object> stats) {
        return "你是一名舆情分析专家。请根据以下数据生成一份简洁的中文舆情分析报告（200-400字），" +
                "包含总体概况、情感倾向分析、热点话题、风险建议四个部分。\n\n数据：" + stats;
    }

    private String callLlmApi(String prompt) throws Exception {
        return callLlmApiWithMessages(List.of(Map.of("role", "user", "content", prompt)));
    }

    private String callLlmApiWithMessages(List<Map<String, String>> messages) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl, new HttpEntity<>(body, headers), String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String content = root.path("choices").path(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("大模型返回内容为空");
        }
        return content;
    }

    private String generateRuleBasedReport(Map<String, Object> stats) {
        int total = ((Number) stats.getOrDefault("totalTexts", 0)).intValue();
        List<Map<String, Object>> sentiment = asMapList(stats.get("sentimentDistribution"));
        List<Map<String, Object>> categories = asMapList(stats.get("categoryDistribution"));
        @SuppressWarnings("unchecked")
        List<HotTopic> hotTopics = stats.get("hotTopics") instanceof List<?> list
                ? (List<HotTopic>) list : List.of();

        int pos = 0, neg = 0, neu = 0;
        if (sentiment != null) {
            for (Map<String, Object> s : sentiment) {
                String name = (String) s.get("name");
                int val = ((Number) s.get("value")).intValue();
                if ("positive".equals(name)) pos = val;
                else if ("negative".equals(name)) neg = val;
                else if ("neutral".equals(name)) neu = val;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【舆情智能分析报告】\n\n");
        sb.append("一、总体概况\n");
        sb.append(String.format("系统当前共收录文本 %d 条，已完成情感与分类分析。\n\n", total));

        sb.append("二、情感倾向分析\n");
        int analyzed = pos + neg + neu;
        if (analyzed > 0) {
            sb.append(String.format("正面 %d 条(%.1f%%)、负面 %d 条(%.1f%%)、中性 %d 条(%.1f%%)。\n",
                    pos, pos * 100.0 / analyzed, neg, neg * 100.0 / analyzed, neu, neu * 100.0 / analyzed));
            if (neg * 1.0 / analyzed > 0.4) {
                sb.append("⚠ 负面情感占比较高，建议重点关注用户投诉与风险舆情。\n");
            } else {
                sb.append("整体舆情偏正面，用户满意度较好。\n");
            }
        }
        sb.append("\n三、热点话题\n");
        if (hotTopics != null && !hotTopics.isEmpty()) {
            for (int i = 0; i < Math.min(5, hotTopics.size()); i++) {
                HotTopic t = hotTopics.get(i);
                sb.append(String.format("  %d. %s（%s次）\n", i + 1,
                        t.getKeyword(), t.getCount()));
            }
        } else {
            sb.append("  暂无足够热点数据，建议增加数据采集。\n");
        }

        sb.append("\n四、分类分布\n");
        if (categories != null) {
            for (Map<String, Object> c : categories) {
                sb.append(String.format("  · %s: %s条\n", c.get("name"), c.get("value")));
            }
        }

        sb.append("\n五、建议\n");
        sb.append("1. 持续监控负面情感变化趋势\n");
        sb.append("2. 针对高频分类加大内容审核力度\n");
        sb.append("3. 配置 LLM API Key 可启用大模型深度解读\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMapList(Object value) {
        if (value instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }
}
