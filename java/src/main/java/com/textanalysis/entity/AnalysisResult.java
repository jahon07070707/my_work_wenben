package com.textanalysis.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AnalysisResult {
    private Long id;
    private Long textId;
    private String category;
    private BigDecimal categoryConfidence;
    private String sentiment;
    private BigDecimal sentimentConfidence;
    private String keywords;
    private LocalDateTime analyzedAt;
    private String title;
    private String content;
}
