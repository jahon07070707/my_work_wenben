package com.textanalysis.mapper;

import com.textanalysis.entity.HotTopic;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface HotTopicMapper {
    @Insert("INSERT INTO hot_topic(keyword, count, stat_date) VALUES(#{keyword}, #{count}, #{statDate}) " +
            "ON DUPLICATE KEY UPDATE count = count + #{count}")
    int upsert(HotTopic topic);

    @Select("SELECT keyword, SUM(count) as count FROM hot_topic WHERE stat_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY keyword ORDER BY count DESC LIMIT #{limit}")
    List<HotTopic> findTop(int limit);
}
