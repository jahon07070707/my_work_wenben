package com.textanalysis.controller;

import com.textanalysis.common.Result;
import com.textanalysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AnalysisService analysisService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.ok(analysisService.getDashboardStats());
    }

    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> getTrend(@RequestParam(defaultValue = "week") String range) {
        return Result.ok(analysisService.getTrendData(range));
    }
}
