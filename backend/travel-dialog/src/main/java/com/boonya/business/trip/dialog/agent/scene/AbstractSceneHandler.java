package com.boonya.business.trip.dialog.agent.scene;

import com.alibaba.fastjson.JSONObject;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import com.boonya.business.trip.common.models.rag.RagPolicyResult;
import com.boonya.business.trip.dialog.chat.Clarify;
import com.boonya.business.trip.dialog.context.DialogContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractSceneHandler implements SceneHandler {

    @Override
    public OrderGenerationResponse handleParameterCollection(DialogContext context,
                                                             String userInput,
                                                             List<?> history,
                                                             RagPolicyResult ragResult) {
        log.info("【{}参数收集】用户输入：{}", getSupportedScene().getDesc(), userInput);

        extractEntities(context, userInput);

        List<String> missingParams = getRequiredParams();
        List<String> questions = new ArrayList<>();

        for (String param : missingParams) {
            Object value = context.getCollectedParam(param);
            if (value == null || "".equals(value.toString().trim())) {
                questions.add(Clarify.getClarificationQuestion(param));
            }
        }

        log.info("【{}参数收集】缺失参数：{}, 待回答问题数：{}",
                getSupportedScene().getDesc(), missingParams, questions.size());

        if (!questions.isEmpty()) {
            OrderGenerationResponse response = new OrderGenerationResponse();
            response.setSessionId(context.getSessionId());
            response.setStatus("clarify");
            response.setMessage(questions.get(0));
            return response;
        }

        log.info("【{}参数收集】参数已完整，生成推荐方案", getSupportedScene().getDesc());

        List<?> solutions = generateSolutions(context, ragResult);
        context.setRecommendedSolutions(solutions);

        OrderGenerationResponse response = new OrderGenerationResponse();
        response.setSessionId(context.getSessionId());
        response.setSolutions(solutions);

        if (solutions != null && !solutions.isEmpty()) {
            StringBuilder messageBuilder = new StringBuilder();
            int total = solutions.size();
            for (int i = 0; i < total; i++) {
                // 实际对象转成Map
                Map<String, Object> solution = JSONObject.parseObject(JSONObject.toJSONString(solutions.get(i)), Map.class);
                messageBuilder.append(solution.get("solutionName")).append("\n");
            }
            response.setStatus("recommend");
            response.setMessage("已为您找到以下方案，请选择（回复编号:1-" + total + "）：\n" + messageBuilder);
        } else {
            response.setStatus("clarify");
            response.setMessage("当前没有找到符合条件的数据推荐，请调整下单要求!");
            return response;
        }

        return response;
    }

    protected abstract void extractEntities(DialogContext context, String userInput);
}
