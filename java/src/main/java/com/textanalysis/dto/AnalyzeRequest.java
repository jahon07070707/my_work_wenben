package com.textanalysis.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AnalyzeRequest {
    private String text;
}

@Data
class AnalyzeResponse {
    private String category;
    private BigDecimal categoryConfidence;
    private Map<String, Double> categoryProbs;
    private String sentiment;
    private String sentimentLabel;
    private BigDecimal sentimentConfidence;
    private Map<String, Double> sentimentProbs;
    private List<String> keywords;
}
