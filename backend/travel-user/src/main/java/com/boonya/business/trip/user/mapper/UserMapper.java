package com.boonya.business.trip.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boonya.business.trip.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("select count(*) from users where deleted=0 and company_id = #{companyId}")
    Long countByCompanyId(Long companyId);

    @Select("select count(*) from users where deleted=0")
    Long count();
}