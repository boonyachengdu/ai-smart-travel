package com.boonya.business.trip.dialog.context;

import com.alibaba.fastjson.JSONObject;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;
import com.boonya.business.trip.dialog.chat.Clarify;
import com.boonya.business.trip.dialog.constant.DialogStage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 对话上下文管理器（管理每轮对话的状态和参数）
 */
@Slf4j
@Data
public class DialogContext {

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 公司 ID
     */
    private Long companyId;

    /**
     * 当前场景
     */
    private Scene scene;

    /**
     * 当前对话阶段
     */
    private DialogStage currentStage = DialogStage.IDLE;

    /**
     * 已收集的结构化参数（不展示给用户，仅用于内部流转）
     */
    private JSONObject collectedParams = new JSONObject();

    /**
     * 待澄清的问题列表
     */
    private List<String> pendingQuestions = new ArrayList<>();

    /**
     * 推荐的方案列表（最多 5 个）
     */
    private List<?> recommendedSolutions = new ArrayList<>();

    /**
     * 用户选择的方案索引
     */
    private Integer selectedSolutionIndex;

    /**
     * RAG 检索到的相关政策
     */
    private String ragPolicyContext;

    /**
     * 是否已进行合规校验
     */
    private boolean complianceChecked = false;

    /**
     * 合规校验结果
     */
    private Boolean complianceResult;

    /**
     * 订单需求参数
     */
    private OrderRequirements orderRequirements;

    /**
     * 添加已收集参数
     */
    public void addCollectedParam(String key, Object value) {
        if (value != null && !"".equals(value.toString().trim())) {
            collectedParams.put(key, value);
            log.debug("收集参数：{} = {}", key, value);
        }
    }

    /**
     * 获取已收集参数
     */
    public Object getCollectedParam(String key) {
        return collectedParams.get(key);
    }

    /**
     * 检查是否缺少必要参数
     */
    public boolean isMissingRequiredParams(String... requiredKeys) {
        for (String key : requiredKeys) {
            if (!collectedParams.containsKey(key)) {
                pendingQuestions.add(Clarify.getClarificationQuestion(key));
                return true;
            }
        }
        return false;
    }


    /**
     * 清空待澄清问题
     */
    public void clearPendingQuestions() {
        this.pendingQuestions.clear();
    }

    /**
     * 推进对话阶段
     */
    public void advanceStage(DialogStage nextStage) {
        log.info("对话阶段推进：{} -> {}", this.currentStage.getDesc(), nextStage.getDesc());
        this.currentStage = nextStage;
    }

    /**
     * 重置上下文（用于新会话或重新开始）
     */
    public void reset() {
        this.currentStage = DialogStage.IDLE;
        this.collectedParams.clear();
        this.pendingQuestions.clear();
        this.recommendedSolutions.clear();
        this.selectedSolutionIndex = null;
        this.complianceChecked = false;
        this.complianceResult = null;
        this.scene = null;
    }
}
