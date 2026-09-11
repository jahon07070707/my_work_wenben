package com.textanalysis.mapper;

import com.textanalysis.entity.RawText;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RawTextMapper {
    @Insert("INSERT INTO raw_text(source_id, task_id, title, content, author, url, publish_time) " +
            "VALUES(#{sourceId}, #{taskId}, #{title}, #{content}, #{author}, #{url}, #{publishTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RawText rawText);

    @Select("SELECT * FROM raw_text WHERE is_analyzed = 0 LIMIT #{limit}")
    List<RawText> findUnanalyzed(int limit);

    @Update("UPDATE raw_text SET is_analyzed = 1 WHERE id = #{id}")
    int markAnalyzed(Long id);

    @Select("SELECT COUNT(*) FROM raw_text")
    int countAll();

    @Select("SELECT * FROM raw_text ORDER BY collected_at DESC LIMIT #{offset}, #{limit}")
    List<RawText> findPage(int offset, int limit);
}
