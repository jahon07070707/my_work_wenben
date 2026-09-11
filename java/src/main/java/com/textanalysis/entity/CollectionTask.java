package com.textanalysis.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CollectionTask {
    private Long id;
    private Long sourceId;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private String sourceName;
}
