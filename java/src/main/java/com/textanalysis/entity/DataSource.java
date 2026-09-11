package com.textanalysis.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DataSource {
    private Long id;
    private String name;
    private String url;
    private String sourceType;
    private String category;
    private Integer status;
    private String description;
    private LocalDateTime createdAt;
}
