package com.boonya.business.trip.common.models.dialog.constant;

/**
 * 意图类型枚举
 */
public enum IntentType {

    // ==================== 对话意图 ====================
    BOOKING("BOOKING","预订流程","进入业务场景"),
    PROVIDING_INFO("PROVIDING_INFO","提供信息","进入业务场景"),
    CONSULTATION("CONSULTATION","咨询","差旅政策咨询或者其他咨询"),
    SOLUTION_RECOMMENDATION("SOLUTION_RECOMMENDATION","提供解决方案","进入业务场景"),

    // ==================== 预订类意图 ====================
    /**
     * 预订机票
     */
    BOOK_FLIGHT("BOOK_FLIGHT", "预订机票", "用户想要预订飞机票"),

    /**
     * 预订酒店
     */
    BOOK_HOTEL("BOOK_HOTEL", "预订酒店", "用户想要预订酒店住宿"),

    /**
     * 预订火车票
     */
    BOOK_TRAIN("BOOK_TRAIN", "预订火车票", "用户想要预订火车票/高铁票"),

    /**
     * 预订用车
     */
    BOOK_CAR("BOOK_CAR", "预订用车", "用户想要预订用车服务"),

    // ==================== 订单管理类意图 ====================
    /**
     * 查询订单
     */
    QUERY_ORDER("QUERY_ORDER", "查询订单", "用户想要查看订单详情或订单列表"),

    /**
     * 取消订单
     */
    CANCEL_ORDER("CANCEL_ORDER", "取消订单", "用户想要取消已存在的订单"),

    /**
     * 修改订单
     */
    MODIFY_ORDER("MODIFY_ORDER", "修改订单", "用户想要修改订单信息"),

    /**
     * 申请报销
     */
    REIMBURSEMENT_APPLY("REIMBURSEMENT_APPLY", "申请报销", "用户想要申请差旅报销"),

    // ==================== 政策咨询类意图 ====================
    /**
     * 查询差旅标准
     */
    QUERY_TRAVEL_STANDARD("QUERY_TRAVEL_STANDARD", "查询差旅标准", "用户询问差旅费用标准、报销标准等"),

    /**
     * 查询审批流程
     */
    QUERY_APPROVAL_PROCESS("QUERY_APPROVAL_PROCESS", "查询审批流程", "用户询问审批流程、审批人等"),

    /**
     * 查询政策规定
     */
    QUERY_POLICY("QUERY_POLICY", "查询政策规定", "用户询问公司差旅政策、规定等"),

    /**
     * 查询额度预算
     */
    QUERY_BUDGET("QUERY_BUDGET", "查询额度预算", "用户询问部门或个人的差旅预算、剩余额度等"),

    // ==================== 审批相关意图 ====================
    /**
     * 审批订单
     */
    APPROVE_ORDER("APPROVE_ORDER", "审批订单", "审批人想要审批待处理的订单"),

    /**
     * 拒绝订单
     */
    REJECT_ORDER("REJECT_ORDER", "拒绝订单", "审批人想要拒绝订单"),

    /**
     * 转交审批
     */
    TRANSFER_APPROVAL("TRANSFER_APPROVAL", "转交审批", "审批人想要将审批转交给他人"),

    // ==================== 帮助与其他意图 ====================
    /**
     * 寻求帮助
     */
    HELP("HELP", "寻求帮助", "用户需要帮助或询问如何使用系统"),

    /**
     * 投诉建议
     */
    COMPLAINT_SUGGESTION("COMPLAINT_SUGGESTION", "投诉建议", "用户想要投诉或提出建议"),

    /**
     * 人工客服
     */
    HUMAN_SERVICE("HUMAN_SERVICE", "人工客服", "用户要求转接人工客服"),

    /**
     * 闲聊问候
     */
    CHITCHAT("CHITCHAT", "闲聊问候", "日常问候、感谢、告别等非业务对话"),

    /**
     * 其他意图
     */
    OTHER("OTHER", "其他意图", "无法归类的其他意图");

    private final String code;
    private final String name;
    private final String description;

    IntentType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据中文名称获取意图类型
     */
    public static IntentType fromName(String name) {
        if (name == null) {
            return OTHER;
        }

        String normalizedName = name.trim();

        // 支持中文名称匹配
        switch (normalizedName) {
            case "预订机票":
            case "订机票":
            case "买机票":
                return BOOK_FLIGHT;
            case "预订酒店":
            case "订酒店":
            case "订房":
                return BOOK_HOTEL;
            case "预订火车票":
            case "订火车票":
            case "买火车票":
            case "订高铁":
                return BOOK_TRAIN;
            case "预订用车":
            case "订车":
            case "叫车":
                return BOOK_CAR;
            case "查询订单":
            case "看订单":
            case "我的订单":
                return QUERY_ORDER;
            case "取消订单":
            case "退订单":
                return CANCEL_ORDER;
            case "修改订单":
            case "改订单":
                return MODIFY_ORDER;
            case "申请报销":
            case "报销":
                return REIMBURSEMENT_APPLY;
            case "查询差旅标准":
            case "差旅标准":
            case "住宿标准":
            case "机票标准":
                return QUERY_TRAVEL_STANDARD;
            case "查询审批流程":
            case "审批流程":
                return QUERY_APPROVAL_PROCESS;
            case "查询政策规定":
            case "政策规定":
            case "差旅政策":
                return QUERY_POLICY;
            case "查询额度预算":
            case "预算":
            case "额度":
                return QUERY_BUDGET;
            case "审批订单":
            case "同意":
            case "通过":
                return APPROVE_ORDER;
            case "拒绝订单":
            case "拒绝":
            case "驳回":
                return REJECT_ORDER;
            case "转交审批":
            case "转交":
                return TRANSFER_APPROVAL;
            case "帮助":
            case "怎么用":
            case "如何使用":
                return HELP;
            case "投诉建议":
            case "投诉":
            case "建议":
                return COMPLAINT_SUGGESTION;
            case "人工客服":
            case "转人工":
            case "人工服务":
                return HUMAN_SERVICE;
            case "闲聊问候":
            case "你好":
            case "谢谢":
            case "再见":
                return CHITCHAT;
            default:
                return OTHER;
        }
    }
}
