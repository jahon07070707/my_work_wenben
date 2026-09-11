package com.textanalysis.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}

@Data
class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String role;
}

@Data
class BatchAnalyzeRequest {
    private java.util.List<String> texts;
}
