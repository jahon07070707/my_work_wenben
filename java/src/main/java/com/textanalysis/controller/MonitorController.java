package com.textanalysis.controller;

import com.textanalysis.common.Result;
import com.textanalysis.service.CustomHealthIndicator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final CustomHealthIndicator healthIndicator;

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(healthIndicator.getSystemStatus());
    }
}
