package com.textanalysis.mapper;

import com.textanalysis.entity.SysUser;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SysUserMapper {
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(String username);

    @Select("SELECT id, username, nickname, role, created_at FROM sys_user WHERE id = #{id}")
    SysUser findById(Long id);
}
