package com.textanalysis.mapper;

import com.textanalysis.entity.DataSource;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DataSourceMapper {
    @Select("SELECT * FROM data_source WHERE status = 1")
    List<DataSource> findAllActive();

    @Select("SELECT * FROM data_source")
    List<DataSource> findAll();

    @Select("SELECT * FROM data_source WHERE id = #{id}")
    DataSource findById(Long id);
}
