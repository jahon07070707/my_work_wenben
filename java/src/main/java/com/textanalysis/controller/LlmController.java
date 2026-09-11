package com.textanalysis.controller;

import com.textanalysis.common.Result;
import com.textanalysis.dto.ChatRequest;
import com.textanalysis.entity.LlmReport;
import com.textanalysis.service.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @PostMapping("/report")
    public Result<Map<String, Object>> generate(@RequestAttribute("username") String username) {
        return Result.ok(llmService.generateReport(username));
    }

    @GetMapping("/reports")
    public Result<List<LlmReport>> history(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(llmService.getHistory(limit));
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        return Result.ok(llmService.getConfig());
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        return Result.ok(llmService.chat(request.getMessages()));
    }
}
