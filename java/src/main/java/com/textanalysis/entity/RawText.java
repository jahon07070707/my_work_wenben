package com.textanalysis.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RawText {
    private Long id;
    private Long sourceId;
    private Long taskId;
    private String title;
    private String content;
    private String author;
    private String url;
    private LocalDateTime publishTime;
    private LocalDateTime collectedAt;
    private Integer isAnalyzed;
}
