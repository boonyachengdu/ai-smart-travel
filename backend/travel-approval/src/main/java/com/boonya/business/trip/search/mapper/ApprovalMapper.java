package com.boonya.business.trip.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boonya.business.trip.common.entity.Approval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ApprovalMapper extends BaseMapper<Approval> {
    @Select("select count(*) from approvals where status = 'PENDING'")
    Long countPendingAll();

    @Select("select count(*) from approvals where status = 'PENDING' and company_id=#{companyId}")
    Long countPendingByCompanyId(Long companyId);
}
