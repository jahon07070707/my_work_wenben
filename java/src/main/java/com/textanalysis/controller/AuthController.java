package com.textanalysis.controller;

import com.textanalysis.common.Result;
import com.textanalysis.dto.LoginRequest;
import com.textanalysis.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return Result.fail("用户名和密码不能为空");
        }
        return Result.ok(authService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestAttribute("userId") Long userId) {
        return Result.ok(authService.getUserInfo(userId));
    }
}
