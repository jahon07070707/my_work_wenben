package com.textanalysis.controller;

import com.textanalysis.common.Result;
import com.textanalysis.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(alertService.getAlerts(limit));
    }

    @PostMapping("/check")
    public Result<String> check() {
        alertService.checkAndGenerateAlerts();
        return Result.ok("预警检测完成");
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        alertService.markRead(id);
        return Result.ok(null);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        alertService.markAllRead();
        return Result.ok(null);
    }
}
