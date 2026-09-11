package com.textanalysis.entity;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HotTopic {
    private Long id;
    private String keyword;
    private Integer count;
    private LocalDate statDate;
}
