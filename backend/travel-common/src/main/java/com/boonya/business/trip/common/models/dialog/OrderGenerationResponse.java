package com.boonya.business.trip.common.models.dialog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderGenerationResponse {
    /**
     * 会话ID
     */
    private Long sessionId;
    /**
     * 订单生成结果
     */
    private String status;              // complete(完成) / clarify(澄清) / violation(违规) / recommend(推荐) / error(错误)
    /**
     * 订单需求数据
     */
    private OrderRequirements order;
    /**
     * 差标 clarification
     */
    private List<String> clarifyQuestions;
    /**
     * 差标不通过原因/违规原因
     */
    private String violationReason;
    /**
     * 差标合规性检查结果
     */
    private String complianceCheck;
    /**
     * 订单需求消息
     */
    private String message;

    /**
     * 订单需求解决方案
     */
    private List<?> solutions;
}
