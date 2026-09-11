package com.textanalysis.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SentimentAlert {
    private Long id;
    private String alertType;
    private String title;
    private String content;
    private String level;
    private Integer status;
    private LocalDateTime createdAt;
}
