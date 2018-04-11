package com.dfire.common.mapper;

import com.dfire.common.entity.HeraLock;
import org.apache.ibatis.annotations.*;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">凌霄</a>
 * @time: Created in 14:25 2018/1/12
 * @desc
 */
public interface HeraLockMapper {

    @Select("SELECT * FROM hera_lock WHERE subgroup = #{subGroup}")
    @Results({
            @Result(id=true, column="id", property = "id"),
            @Result(column="gmt_create", property = "gmtCreate"),
            @Result(column="gmt_modified", property = "gmtModified"),
            @Result(column="host", property = "host"),
            @Result(column="server_update", property = "serverUpdate"),
            @Result(column="subgroup", property = "subGroup")
    })
    public HeraLock getHeraLock(@Param("subGroup") String subGroup) ;

    @Update("update ZEUS_LOCK set gmt_create= #{gmtCreate},gmt_modified = #{gmtModified},host = #{host},server_update = #{serverUpdate},subgroup = #{subGroup}  where id = #{id}")
    public void save(HeraLock heraLock);
}