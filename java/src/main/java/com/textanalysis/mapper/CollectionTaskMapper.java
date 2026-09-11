package com.textanalysis.mapper;

import com.textanalysis.entity.CollectionTask;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CollectionTaskMapper {
    @Insert("INSERT INTO collection_task(source_id, status, started_at) VALUES(#{sourceId}, #{status}, #{startedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CollectionTask task);

    @Update("UPDATE collection_task SET status=#{status}, total_count=#{totalCount}, success_count=#{successCount}, " +
            "error_msg=#{errorMsg}, finished_at=#{finishedAt} WHERE id=#{id}")
    int update(CollectionTask task);

    @Select("SELECT t.*, s.name as source_name FROM collection_task t LEFT JOIN data_source s ON t.source_id = s.id " +
            "ORDER BY t.created_at DESC LIMIT #{limit}")
    List<CollectionTask> findRecent(int limit);
}
