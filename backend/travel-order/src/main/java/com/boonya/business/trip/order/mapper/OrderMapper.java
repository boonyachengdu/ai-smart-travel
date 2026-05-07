package com.boonya.business.trip.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boonya.business.trip.common.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    /**
     * 统计订单数量
     *
     * @return 订单数量
     */
    @Select("SELECT COUNT(*) FROM orders where deleted = 0 ")
    Long countAllOrders();

    /**
     * 统计订单数量
     *
     * @return 订单数量
     */
    @Select("SELECT COUNT(*) FROM orders where deleted = 0 and company_id =#{companyId} ")
    Long countByCompanyId(Long companyId);

    /**
     * 按月统计订单数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM orders " +
            "WHERE deleted = 0 " +
            "<if test='companyId != null'>" +
            "AND company_id = #{companyId}" +
            "</if>" +
            "AND create_time &gt;= #{startOfMonth}" +
            "</script>")
    Long countByMonth(@Param("companyId") Long companyId, @Param("startOfMonth") LocalDateTime startOfMonth);

}