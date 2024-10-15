package com.pcl.simpleweb.mapper;

import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * @ClassName UserMapper
 * @Description TODO
 * @Author Chenglin Peng
 * @Data 2024/10/11 18:24
 * @Version F02SP03
 **/
@Repository
public interface UserMapper {
    @Select("select username from admin limit 1")
    public String getUserName();

}
