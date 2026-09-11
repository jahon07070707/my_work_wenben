package com.textanalysis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final RestTemplate restTemplate;

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean dbOk = checkDatabase(details);
        boolean mlOk = checkMlService(details);
        details.put("status", dbOk && mlOk ? "UP" : "DEGRADED");

        if (dbOk && mlOk) {
            return Health.up().withDetails(details).build();
        }
        return Health.status("DEGRADED").withDetails(details).build();
    }

    public Map<String, Object> getSystemStatus() {
        Map<String, Object> details = new HashMap<>();
        checkDatabase(details);
        checkMlService(details);
        details.put("java", Map.of("status", "UP", "port", 8080));
        details.put("timestamp", System.currentTimeMillis());
        return details;
    }

    private boolean checkDatabase(Map<String, Object> details) {
        try (Connection conn = dataSource.getConnection()) {
            details.put("mysql", Map.of("status", "UP", "catalog", conn.getCatalog()));
            return true;
        } catch (Exception e) {
            details.put("mysql", Map.of("status", "DOWN", "error", e.getMessage()));
            return false;
        }
    }

    private boolean checkMlService(Map<String, Object> details) {
        try {
            Map<?, ?> health = restTemplate.getForObject(mlServiceUrl + "/health", Map.class);
            boolean loaded = health != null && Boolean.TRUE.equals(health.get("model_loaded"));
            details.put("mlService", Map.of(
                    "status", health != null ? "UP" : "DOWN",
                    "url", mlServiceUrl,
                    "modelLoaded", loaded
            ));
            return health != null;
        } catch (Exception e) {
            details.put("mlService", Map.of("status", "DOWN", "error", e.getMessage()));
            return false;
        }
    }
}
