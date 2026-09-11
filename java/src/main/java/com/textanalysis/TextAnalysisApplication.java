package com.textanalysis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.textanalysis.mapper")
public class TextAnalysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(TextAnalysisApplication.class, args);
    }
}
