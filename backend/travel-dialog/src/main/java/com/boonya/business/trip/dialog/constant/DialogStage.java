package com.boonya.business.trip.dialog.constant;

public enum DialogStage {
    /**
     * 初始/空闲状态
     */
    IDLE("IDLE", "空闲"),

    /**
     * 意图识别：判断用户要做什么（预订/咨询/闲聊）
     */
    INTENT_RECOGNITION("INTENT_RECOGNITION", "意图识别"),

    /**
     * 咨询问答：回答政策、流程等问题（RAG）
     */
    CONSULTATION("CONSULTATION", "咨询问答"),

    /**
     * 出差申请：用户表达出差需求
     */
    TRIP_APPLICATION("TRIP_APPLICATION", "出差申请"),

    /**
     * 参数收集：逐步收集必要参数
     */
    PARAMETER_COLLECTION("PARAMETER_COLLECTION", "参数收集"),

    /**
     * 方案推荐：提供多个选项供用户选择
     */
    SOLUTION_RECOMMENDATION("SOLUTION_RECOMMENDATION", "方案推荐"),

    /**
     * 特征选择：用户选择具体特征（舱位、房型等）
     */
    FEATURE_SELECTION("FEATURE_SELECTION", "特征选择"),

    /**
     * 合规校验：RAG 差标校验
     */
    COMPLIANCE_CHECK("COMPLIANCE_CHECK", "合规校验"),

    /**
     * 最终确认：用户确认下单
     */
    FINAL_CONFIRMATION("FINAL_CONFIRMATION", "最终确认"),

    /**
     * 下单完成
     */
    ORDER_COMPLETED("ORDER_COMPLETED", "下单完成");

    private final String code;
    private final String desc;

    DialogStage(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
