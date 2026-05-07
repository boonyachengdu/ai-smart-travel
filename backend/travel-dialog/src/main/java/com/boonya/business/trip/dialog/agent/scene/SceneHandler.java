package com.boonya.business.trip.dialog.agent.scene;

import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import com.boonya.business.trip.common.models.rag.RagPolicyResult;
import com.boonya.business.trip.dialog.context.DialogContext;

import java.util.List;
import java.util.Map;

/**
 * 业务场景处理器
 */
public interface SceneHandler {

    /**
     * 获取支持的业务场景
     *
     * @return
     */
    Scene getSupportedScene();

    /**
     * 获取必须的参数
     *
     * @return
     */
    List<String> getRequiredParams();

    /**
     * 获取初始问题
     *
     * @param context
     * @return
     */
    String getInitialQuestion(DialogContext context);

    /**
     * 获取订单生成提示词
     *
     * @return
     */
    String getOrderPrompt();

    /**
     * 获取意图识别提示词
     * @param context
     * @param userInput
     * @return
     */
    String getIntentRecognitionPrompt(DialogContext context, String userInput);

    /**
     * 处理参数收集
     *
     * @param context
     * @param userInput
     * @param history
     * @param ragResult
     * @return
     */
    OrderGenerationResponse handleParameterCollection(DialogContext context,
                                                      String userInput,
                                                      List<?> history,
                                                      RagPolicyResult ragResult);

    /**
     * 生成解决方案
     *
     * @param context
     * @param ragResult
     * @return
     */
    List<?> generateSolutions(DialogContext context, RagPolicyResult ragResult);

    /**
     * 构建订单摘要
     *
     * @param context
     * @return
     */
    String buildOrderSummary(DialogContext context);

    /**
     * 提取解决方案详情
     *
     * @param solution
     * @return
     */
    Map<String, Object> extractSolutionDetails(Object solution);

    default boolean supports(Scene scene) {
        return getSupportedScene() == scene;
    }
}
