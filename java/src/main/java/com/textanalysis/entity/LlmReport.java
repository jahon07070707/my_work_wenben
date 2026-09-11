package com.textanalysis.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LlmReport {
    private Long id;
    private String reportType;
    private String content;
    private String createdBy;
    private LocalDateTime createdAt;
}
