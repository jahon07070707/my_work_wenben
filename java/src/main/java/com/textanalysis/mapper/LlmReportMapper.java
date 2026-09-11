package com.textanalysis.mapper;

import com.textanalysis.entity.LlmReport;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface LlmReportMapper {
    @Insert("INSERT INTO llm_report(report_type, content, created_by) VALUES(#{reportType}, #{content}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LlmReport report);

    @Select("SELECT * FROM llm_report ORDER BY created_at DESC LIMIT #{limit}")
    List<LlmReport> findRecent(int limit);
}
