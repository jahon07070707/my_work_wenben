package com.textanalysis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final SimpMessagingTemplate messagingTemplate;

    public void pushTaskProgress(Long taskId, String status, String message, int progress) {
        messagingTemplate.convertAndSend("/topic/task-progress", Map.of(
                "taskId", taskId,
                "status", status,
                "message", message,
                "progress", progress,
                "timestamp", System.currentTimeMillis()
        ));
    }

    public void pushAlert(String title, String level) {
        messagingTemplate.convertAndSend("/topic/alerts", Map.of(
                "title", title,
                "level", level,
                "timestamp", System.currentTimeMillis()
        ));
    }
}
