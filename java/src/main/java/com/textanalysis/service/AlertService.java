package com.textanalysis.service;

import com.textanalysis.entity.SentimentAlert;
import com.textanalysis.mapper.AnalysisResultMapper;
import com.textanalysis.mapper.SentimentAlertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final SentimentAlertMapper alertMapper;
    private final AnalysisResultMapper analysisResultMapper;
    private final WebSocketPushService pushService;

    @Value("${alert.negative-ratio-threshold:0.4}")
    private double negativeRatioThreshold;

    @Value("${alert.negative-count-threshold:5}")
    private int negativeCountThreshold;

    public void checkAndGenerateAlerts() {
        List<Map<String, Object>> sentimentDist = analysisResultMapper.countBySentiment();
        int positive = 0, negative = 0, neutral = 0;
        for (Map<String, Object> item : sentimentDist) {
            String name = (String) item.get("name");
            int value = ((Number) item.get("value")).intValue();
            switch (name) {
                case "positive" -> positive = value;
                case "negative" -> negative = value;
                case "neutral" -> neutral = value;
            }
        }
        int total = positive + negative + neutral;
        if (total == 0) return;

        double negRatio = (double) negative / total;
        if (negative >= negativeCountThreshold && negRatio >= negativeRatioThreshold) {
            createAlert("negative_ratio", "负面舆情预警",
                    String.format("近期负面情感占比 %.1f%%（%d/%d条），已超过阈值 %.0f%%",
                            negRatio * 100, negative, total, negativeRatioThreshold * 100),
                    negRatio > 0.6 ? "danger" : "warning");
        }

        List<Map<String, Object>> categories = analysisResultMapper.countByCategory();
        for (Map<String, Object> cat : categories) {
            String name = (String) cat.get("name");
            int value = ((Number) cat.get("value")).intValue();
            if (value >= 10) {
                createAlert("category_spike", "分类热点提醒",
                        String.format("「%s」类文本已达 %d 条，建议重点关注", name, value), "info");
            }
        }
    }

    private void createAlert(String type, String title, String content, String level) {
        SentimentAlert alert = new SentimentAlert();
        alert.setAlertType(type);
        alert.setTitle(title);
        alert.setContent(content);
        alert.setLevel(level);
        alertMapper.insert(alert);
        pushService.pushAlert(title, level);
        log.info("Alert created: {}", title);
    }

    public Map<String, Object> getAlerts(int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", alertMapper.findRecent(limit));
        result.put("unreadCount", alertMapper.countUnread());
        return result;
    }

    public void markRead(Long id) {
        alertMapper.markRead(id);
    }

    public void markAllRead() {
        alertMapper.markAllRead();
    }
}
