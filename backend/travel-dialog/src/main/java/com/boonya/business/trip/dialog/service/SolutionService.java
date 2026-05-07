package com.boonya.business.trip.dialog.service;

import com.alibaba.dashscope.common.Message;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import com.boonya.business.trip.common.models.rag.RagPolicyResult;
import com.boonya.business.trip.dialog.context.DialogContext;
import com.boonya.business.trip.dialog.vo.ChatMessageVO;

import java.util.List;

public interface SolutionService {
    /**
     * 推荐方案
     * @param context
     * @param userInput
     * @param history
     * @param ragResult
     * @return
     */
    OrderGenerationResponse handleSolutionRecommendationStage(DialogContext context, String userInput,
                                                              List<ChatMessageVO> history, RagPolicyResult ragResult);

    /**
     * 生成方案
     * @param context
     * @param ragResult
     * @return
     */
    List<?> generateSolutions(DialogContext context, RagPolicyResult ragResult);
}
