package com.textanalysis.mapper;

import com.textanalysis.entity.SentimentAlert;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SentimentAlertMapper {
    @Insert("INSERT INTO sentiment_alert(alert_type, title, content, level) VALUES(#{alertType}, #{title}, #{content}, #{level})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SentimentAlert alert);

    @Select("SELECT * FROM sentiment_alert ORDER BY created_at DESC LIMIT #{limit}")
    List<SentimentAlert> findRecent(int limit);

    @Select("SELECT COUNT(*) FROM sentiment_alert WHERE status = 0")
    int countUnread();

    @Update("UPDATE sentiment_alert SET status = 1 WHERE id = #{id}")
    int markRead(Long id);

    @Update("UPDATE sentiment_alert SET status = 1 WHERE status = 0")
    int markAllRead();
}
