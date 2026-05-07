package com.boonya.business.trip.dialog.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.boonya.business.trip.common.models.dialog.Intent;
import com.boonya.business.trip.common.models.dialog.constant.IntentType;
import com.boonya.business.trip.dialog.context.DialogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiIntentHandler {
    private final ChatClient chatClient;

    public List<Intent> parseMultiIntent(String userInput, String sessionId) {
        try {
            String systemPrompt = getSystemPrompt();
            String userPrompt = buildMultiIntentPrompt(userInput);

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .advisors(advisor -> advisor.param("chatMemoryContextId", "multi_intent_" +sessionId))
                    .call()
                    .content();

            log.debug("多意图识别响应：{}", response);

            return parseIntentList(response);

        } catch (Exception e) {
            log.error("多意图识别失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 处理多个意图
     *
     * @param intents 意图列表
     * @param context 对话上下文
     */
    public void handleMultiIntent(List<Intent> intents, DialogContext context) {
        if (intents == null || intents.isEmpty()) {
            log.warn("没有需要处理的意图");
            return;
        }

        log.info("开始处理 {} 个意图", intents.size());

        // 按优先级排序
        intents.sort((i1, i2) -> {
            int p1 = i1.getPriority() != null ? i1.getPriority() : 999;
            int p2 = i2.getPriority() != null ? i2.getPriority() : 999;
            return Integer.compare(p1, p2);
        });

        // 按优先级处理每个意图
        for (Intent intent : intents) {
            log.info("处理意图：{} (置信度：{})", intent.getIntentName(), intent.getConfidence());

            try {
                processSingleIntent(intent, context);
            } catch (Exception e) {
                log.error("处理意图 {} 失败", intent.getIntentName(), e);
            }
        }
    }

    /**
     * 构建多意图识别的提示词
     */
    private String buildMultiIntentPrompt(String userInput) {
        return """
            分析以下用户输入，识别包含的所有意图。
            
            可能的意图类型包括：
            1. 预订类：预订机票、预订酒店、预订火车票、预订用车
            2. 订单类：查询订单、取消订单、修改订单、申请报销
            3. 政策类：查询差旅标准、查询审批流程、查询政策规定、查询额度预算
            4. 审批类：审批订单、拒绝订单、转交审批
            5. 其他：寻求帮助、投诉建议、人工客服、闲聊问候
            
            用户输入：%s
            
            请输出 JSON 数组格式，每个意图包含以下字段：
            - intent: 意图类型（使用英文代码，如 BOOK_FLIGHT）
            - intentName: 意图中文名称（如"预订机票"）
            - confidence: 置信度（0.0-1.0）
            - entities: 提取的实体信息（JSON 对象）
            - priority: 优先级（1-5，1 最高）
            - reason: 判断理由
            
            示例：
            用户："帮我订明天北京到上海的机票，顺便查一下差旅标准"
            输出：[
              {
                "intent": "BOOK_FLIGHT",
                "intentName": "预订机票",
                "confidence": 0.95,
                "entities": {
                  "departureCity": "北京",
                  "arrivalCity": "上海",
                  "departureDate": "明天"
                },
                "priority": 1,
                "reason": "明确表达预订机票需求，并提供关键信息"
              },
              {
                "intent": "QUERY_TRAVEL_STANDARD",
                "intentName": "查询差旅标准",
                "confidence": 0.90,
                "entities": {},
                "priority": 2,
                "reason": "询问差旅标准相关政策"
              }
            ]
            """.formatted(userInput);
    }

    /**
     * 获取系统提示词
     */
    private String getSystemPrompt() {
        return """
            你是一个专业的智能商旅助手意图识别专家。
            你的任务是准确识别用户输入中包含的所有意图。
            
            要求：
            1. 必须输出纯 JSON 数组，不要任何多余的文字、markdown 标记或解释
            2. 置信度低于 0.5 的意图不要包含在结果中
            3. 优先级规则：预订类 > 订单类 > 审批类 > 政策类 > 其他
            4. 实体提取要准确，包括城市、日期、订单号等关键信息
            """;
    }

    /**
     * 解析意图列表
     */
    @SuppressWarnings("unchecked")
    private List<Intent> parseIntentList(String response) {
        List<Intent> intents = new ArrayList<>();

        if (response == null || response.trim().isEmpty()) {
            log.warn("意图识别响应为空");
            return intents;
        }

        try {
            // 提取 JSON 数组部分
            String jsonContent = extractJsonArray(response);

            JSONArray jsonArray = JSON.parseArray(jsonContent);

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                Intent intent = Intent.builder()
                        .intentType(parseIntentType(jsonObject))
                        .intentName(jsonObject.getString("intentName"))
                        .confidence(jsonObject.getDouble("confidence"))
                        .entities(jsonObject.getJSONObject("entities") != null ?
                                (Map<String, Object>) jsonObject.getJSONObject("entities").toJavaObject(Map.class) :
                                new HashMap<>())
                        .priority(jsonObject.getInteger("priority"))
                        .reason(jsonObject.getString("reason"))
                        .build();

                intents.add(intent);
            }

            log.info("成功解析 {} 个意图", intents.size());

        } catch (Exception e) {
            log.error("解析意图列表失败：{}", response, e);
        }

        return intents;
    }

    /**
     * 解析意图类型
     */
    private IntentType parseIntentType(JSONObject jsonObject) {
        // 尝试从 intent 字段解析
        String intentStr = jsonObject.getString("intent");
        if (intentStr != null && !intentStr.trim().isEmpty()) {
            // 如果是英文代码
            try {
                return IntentType.valueOf(intentStr.trim());
            } catch (IllegalArgumentException e) {
                // 如果不是枚举值，尝试从中文名称转换
                return IntentType.fromName(intentStr);
            }
        }

        // 尝试从 intentName 字段解析
        String intentName = jsonObject.getString("intentName");
        if (intentName != null && !intentName.trim().isEmpty()) {
            return IntentType.fromName(intentName);
        }

        return IntentType.OTHER;
    }

    /**
     * 处理单个意图
     */
    private void processSingleIntent(Intent intent, DialogContext context) {
        IntentType intentType = intent.getIntentType();

        switch (intentType) {
            // ==================== 预订类意图 ====================
            case BOOK_FLIGHT:
                context.addCollectedParam("orderType", "FLIGHT");
                context.setScene(com.boonya.business.trip.common.constant.Scene.FLIGHT);
                extractEntitiesToContext(intent.getEntities(), context);
                break;

            case BOOK_HOTEL:
                context.addCollectedParam("orderType", "HOTEL");
                context.setScene(com.boonya.business.trip.common.constant.Scene.HOTEL);
                extractEntitiesToContext(intent.getEntities(), context);
                break;

            case BOOK_TRAIN:
                context.addCollectedParam("orderType", "TRAIN");
                context.setScene(com.boonya.business.trip.common.constant.Scene.TRAIN);
                extractEntitiesToContext(intent.getEntities(), context);
                break;

            case BOOK_CAR:
                context.addCollectedParam("orderType", "CAR");
                context.setScene(com.boonya.business.trip.common.constant.Scene.CAR);
                extractEntitiesToContext(intent.getEntities(), context);
                break;

            // ==================== 订单管理类意图 ====================
            case QUERY_ORDER:
                // 将意图标记写入上下文，主流程 handleConsultation 会读取并调用订单查询
                context.addCollectedParam("pendingIntent", IntentType.QUERY_ORDER.name());
                if (intent.getEntities() != null) {
                    extractEntitiesToContext(intent.getEntities(), context);
                }
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                log.info("意图：查询订单 → 进入咨询问答阶段");
                break;

            case CANCEL_ORDER:
                context.addCollectedParam("pendingIntent", IntentType.CANCEL_ORDER.name());
                if (intent.getEntities() != null) {
                    extractEntitiesToContext(intent.getEntities(), context);
                }
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                log.info("意图：取消订单 → 进入咨询问答阶段");
                break;

            case MODIFY_ORDER:
                context.addCollectedParam("pendingIntent", IntentType.MODIFY_ORDER.name());
                if (intent.getEntities() != null) {
                    extractEntitiesToContext(intent.getEntities(), context);
                }
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                log.info("意图：修改订单 → 进入咨询问答阶段");
                break;

            case REIMBURSEMENT_APPLY:
                context.addCollectedParam("pendingIntent", IntentType.REIMBURSEMENT_APPLY.name());
                if (intent.getEntities() != null) {
                    extractEntitiesToContext(intent.getEntities(), context);
                }
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                log.info("意图：申请报销 → 进入咨询问答阶段");
                break;

            // ==================== 政策咨询类意图 ====================
            case QUERY_TRAVEL_STANDARD:
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                break;

            case QUERY_APPROVAL_PROCESS:
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                break;

            case QUERY_POLICY:
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                break;

            case QUERY_BUDGET:
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                break;

            // ==================== 审批相关意图 ====================
            case APPROVE_ORDER:
                context.addCollectedParam("pendingIntent", IntentType.APPROVE_ORDER.name());
                if (intent.getEntities() != null) {
                    extractEntitiesToContext(intent.getEntities(), context);
                }
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                log.info("意图：审批订单 → 进入咨询问答阶段（审批操作需前端跳转）");
                break;

            case REJECT_ORDER:
                context.addCollectedParam("pendingIntent", IntentType.REJECT_ORDER.name());
                if (intent.getEntities() != null) {
                    extractEntitiesToContext(intent.getEntities(), context);
                }
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                log.info("意图：拒绝审批 → 进入咨询问答阶段");
                break;

            case TRANSFER_APPROVAL:
                context.addCollectedParam("pendingIntent", IntentType.TRANSFER_APPROVAL.name());
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                log.info("意图：转交审批 → 进入咨询问答阶段");
                break;

            // ==================== 帮助与其他意图 ====================
            case HELP:
                context.addCollectedParam("pendingIntent", IntentType.HELP.name());
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                log.info("意图：寻求帮助 → 进入咨询问答阶段");
                break;

            case COMPLAINT_SUGGESTION:
                context.addCollectedParam("pendingIntent", IntentType.COMPLAINT_SUGGESTION.name());
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                log.info("意图：投诉建议 → 进入咨询问答阶段");
                break;

            case HUMAN_SERVICE:
                context.addCollectedParam("pendingIntent", IntentType.HUMAN_SERVICE.name());
                context.advanceStage(com.boonya.business.trip.dialog.constant.DialogStage.CONSULTATION);
                log.info("意图：人工客服 → 进入咨询问答阶段，提示用户转接");
                break;

            case CHITCHAT:
                // 闲聊不需要特殊处理
                log.debug("闲聊意图，无需特殊处理");
                break;

            case OTHER:
            default:
                log.warn("未知意图：{}", intentType);
                break;
        }
    }

    /**
     * 提取实体信息到上下文
     */
    private void extractEntitiesToContext(Map<String, Object> entities, DialogContext context) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : entities.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value != null) {
                context.addCollectedParam(key, value);
                log.debug("提取实体：{} = {}", key, value);
            }
        }
    }

    /**
     * 从响应中提取 JSON 数组
     */
    private String extractJsonArray(String response) {
        if (response == null) {
            return "[]";
        }

        // 查找第一个 [ 和最后一个 ] 的位置
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        log.warn("未能在响应中找到 JSON 数组：{}", response);
        return "[]";
    }
}
