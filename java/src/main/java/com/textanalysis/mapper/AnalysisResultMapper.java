package com.textanalysis.mapper;

import com.textanalysis.entity.AnalysisResult;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisResultMapper {
    @Insert("INSERT INTO analysis_result(text_id, category, category_confidence, sentiment, sentiment_confidence, keywords) " +
            "VALUES(#{textId}, #{category}, #{categoryConfidence}, #{sentiment}, #{sentimentConfidence}, #{keywords})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnalysisResult result);

    List<AnalysisResult> findWithText(@Param("offset") int offset, @Param("limit") int limit,
                                      @Param("category") String category, @Param("sentiment") String sentiment);

    int countWithFilter(@Param("category") String category, @Param("sentiment") String sentiment);

    List<Map<String, Object>> countByCategory();

    List<Map<String, Object>> countBySentiment();

    List<Map<String, Object>> countByDate(@Param("days") int days);

    List<Map<String, Object>> countByHour();

    List<AnalysisResult> findAllForExport(@Param("category") String category, @Param("sentiment") String sentiment);

    List<Map<String, Object>> getWordCloudData(@Param("limit") int limit);
}
