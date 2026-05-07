package com.boonya.business.trip.dialog.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.boonya.business.trip.common.Response;
import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.constant.SessionStatus;
import com.boonya.business.trip.common.context.UserHolder;
import com.boonya.business.trip.common.entity.PolicyDocument;
import com.boonya.business.trip.common.entity.Standard;
import com.boonya.business.trip.common.models.ComplianceResult;
import com.boonya.business.trip.common.models.dialog.Intent;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;
import com.boonya.business.trip.common.models.dialog.OrderGenerationResponse;
import com.boonya.business.trip.common.models.dialog.constant.IntentType;
import com.boonya.business.trip.common.models.rag.PolicyDocumentQueryRequest;
import com.boonya.business.trip.common.models.rag.RagPolicyResult;
import com.boonya.business.trip.dialog.chat.Clarify;
import com.boonya.business.trip.dialog.constant.DialogStage;
import com.boonya.business.trip.common.entity.ChatMessage;
import com.boonya.business.trip.common.entity.ChatSession;
import com.boonya.business.trip.dialog.handler.MultiIntentHandler;
import com.boonya.business.trip.dialog.mapper.ChatMessageMapper;
import com.boonya.business.trip.dialog.mapper.ChatSessionMapper;
import com.boonya.business.trip.dialog.context.DialogContext;
import com.boonya.business.trip.dialog.agent.prompt.ScenePromptService;
import com.boonya.business.trip.dialog.agent.scene.SceneHandler;
import com.boonya.business.trip.dialog.agent.scene.SceneHandlerFactory;
import com.boonya.business.trip.dialog.vo.ChatMessageVO;
import com.boonya.business.trip.feign.clients.OrderFeignClient;
import com.boonya.business.trip.feign.clients.PolicyDocumentFeignClient;
import com.boonya.business.trip.dialog.service.StandardService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DialogSessionService {

    public static final String USER_CHAT_SESSION = "user_chat_session:%s:%s";
    public static final String USER_DIALOG_CONTEXT = "user_dialog_context:%s:%s";
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, List<ChatMessageVO>> redisTemplate;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final PolicyDocumentFeignClient policyDocumentFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final SolutionService solutionService;
    private final ComplianceService complianceService;
    private final SceneHandlerFactory sceneHandlerFactory;
    private final ScenePromptService scenePromptService;
    private final ChatClient chatClient;
    private final MultiIntentHandler multiIntentHandler;
    private final StandardService standardService;

    private static final String SESSION_KEY_PREFIX = "dialog:session:";
    private static final Duration SESSION_TTL = Duration.ofDays(7);

    // 在 DialogSessionService 中注入
    private final ResourceLoader resourceLoader;

    public Long createSession(Scene scene, Long userId, Long companyId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId != null ? userId : 1L);
        session.setCompanyId(companyId != null ? companyId : 1L);
        session.setScene(scene);
        session.setTitle(scene.getDesc() + "-" + LocalDateTime.now());
        session.setStatus(SessionStatus.ACTIVE);
        session.setCurrentStage(DialogStage.IDLE.getCode());
        chatSessionMapper.insert(session);

        log.info("创建新会话：sessionId={}, scene={}", session.getId(), scene);
        String key = String.format(USER_CHAT_SESSION, scene, userId);
        stringRedisTemplate.opsForValue().set(key, session.getId().toString());
        return session.getId();
    }

    public OrderGenerationResponse chat(Scene scene, boolean createNewSession, String userInput) {
        Long sessionId;
        if (createNewSession) {
            log.info("创建新会话：user={}", UserHolder.get());
            sessionId = createSession(scene, UserHolder.get().getUserId(), UserHolder.get().getCompanyId());
        } else {
            String key = String.format(USER_CHAT_SESSION, scene, UserHolder.get().getUserId());
            if (!stringRedisTemplate.hasKey(key)) {
                log.info("创建新会话：user={}", UserHolder.get());
                sessionId = createSession(scene, UserHolder.get().getUserId(), UserHolder.get().getCompanyId());
            } else {
                sessionId = Long.parseLong(stringRedisTemplate.opsForValue().get(key));
            }
        }
        String redisKey = SESSION_KEY_PREFIX + sessionId;

        long startTime = System.currentTimeMillis();
        DialogContext context = initOrLoadContext(scene, sessionId);
        RagPolicyResult ragResult = callRagPolicy(scene, userInput, sessionId);
        context.setRagPolicyContext(ragResult.getPolicyText());
        long endTime = System.currentTimeMillis();
        log.info("1.政策调用耗时：{}ms", endTime - startTime);

        startTime = System.currentTimeMillis();
        List<ChatMessageVO> history = loadHistory(redisKey, sessionId, context);
        endTime = System.currentTimeMillis();
        log.info("2.加载历史耗时：{}ms", endTime - startTime);
        log.info("历史消息：{}", history);

        startTime = System.currentTimeMillis();
        OrderGenerationResponse response = processIntelligently(context, userInput, history, ragResult);
        endTime = System.currentTimeMillis();
        log.info("3.智能处理耗时：{}ms", endTime - startTime);

        startTime = System.currentTimeMillis();
        saveContext(context, redisKey, sessionId, userInput, response);
        endTime = System.currentTimeMillis();
        log.info("4.保存上下文耗时：{}ms", endTime - startTime);

        saveDialogContextToRedis(context, scene, sessionId);
        log.info("5.保存会话上下文：{}", context);

        return response;
    }

    private void saveDialogContextToRedis(DialogContext context, Scene scene, Long sessionId) {
        String key = String.format(USER_DIALOG_CONTEXT, scene, sessionId);
        stringRedisTemplate.opsForValue().set(key, JSONObject.toJSONString(context));
    }

    private DialogContext initOrLoadContext(Scene scene, Long sessionId) {
        String key = String.format(USER_DIALOG_CONTEXT, scene, sessionId);
        String jsonContext = stringRedisTemplate.opsForValue().get(key);
        if (jsonContext != null) {
            DialogContext context = JSONObject.parseObject(jsonContext, DialogContext.class);
            log.info("加载会话上下文：{}", context);
            return context;
        }

        DialogContext context = new DialogContext();
        context.setSessionId(sessionId);
        context.setScene(scene);

        UserHolder userContext = UserHolder.get();
        context.setUserId(userContext.getUserId());
        context.setCompanyId(userContext.getCompanyId());

        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null && session.getCurrentStage() != null) {
            try {
                DialogStage stage = DialogStage.valueOf(session.getCurrentStage());
                context.setCurrentStage(stage);
                log.info("加载会话上下文，当前阶段：{}", stage.getDesc());
            } catch (IllegalArgumentException e) {
                context.setCurrentStage(DialogStage.IDLE);
            }
        } else {
            context.setCurrentStage(DialogStage.IDLE);
        }

        return context;
    }

    private List<ChatMessageVO> loadHistory(String redisKey, Long sessionId, DialogContext context) {
        List<ChatMessageVO> history = redisTemplate.opsForValue().get(redisKey);

        if (history == null || history.isEmpty()) {
            history = loadHistoryFromDB(sessionId);

            if (history.isEmpty()) {
                history = new ArrayList<>();
                String systemPrompt = scenePromptService.buildPrompt(context.getScene(), context.getRagPolicyContext());
                history.add(ChatMessageVO.builder()
                        .role("SYSTEM")
                        .content(systemPrompt)
                        .build());
                log.info("【加载历史】新建会话，添加System Prompt");
            } else {
                log.info("【加载历史】从数据库加载 {} 条消息", history.size());
            }
        } else {
            log.info("【加载历史】从Redis加载 {} 条消息", history.size());
        }

        updateSystemMessage(history, scenePromptService.buildPrompt(context.getScene(), context.getRagPolicyContext()));
        return history;
    }

    private void updateSystemMessage(List<ChatMessageVO> history, String systemPrompt) {
        for (int i = 0; i < history.size(); i++) {
            if ("SYSTEM".equals(history.get(i).getRole())) {
                history.set(i, ChatMessageVO.builder()
                        .role("SYSTEM")
                        .content(systemPrompt)
                        .build());
                break;
            }
        }
    }

    private OrderGenerationResponse processIntelligently(DialogContext context, String userInput,
                                                         List<ChatMessageVO> history, RagPolicyResult ragResult) {

        boolean isInBusinessFlow = isInBusinessFlow(context.getCurrentStage());

        IntentRecognitionResult intentResult = recognizeIntentByLLM(context, userInput, history);

        log.info("【意图识别】用户输入：{} | 识别结果：intent={}, confidence={} | 当前阶段：{}",
                userInput, intentResult.getIntentType(), intentResult.getConfidence(), context.getCurrentStage());

        if (context.getCurrentStage() == DialogStage.PARAMETER_COLLECTION) {
            log.info("【参数收集阶段】结合历史对话提取完整信息");
            restoreParamsFromHistory(context, history);
            return handleParameterCollectionStage(context, userInput, history, ragResult);
        }

        switch (intentResult.getIntentType()) {
            case BOOKING:
            case PROVIDING_INFO:
                return handleBookingIntent(context, userInput, history, ragResult);
            case CONSULTATION:
                return handleConsultationStage(context, userInput, history, ragResult);
            case CHITCHAT:
                if (isInBusinessFlow) {
                    return handleChitChatDuringBusiness(context, userInput);
                } else {
                    return handleChitChat(context, userInput);
                }
            default:
                return processByStage(context, userInput, history, ragResult);
        }
    }

    private void restoreParamsFromHistory(DialogContext context, List<ChatMessageVO> history) {
        log.info("【恢复上下文】开始从历史消息中提取参数...");

        for (ChatMessageVO msg : history) {
            if ("USER".equals(msg.getRole())) {
                String historicalInput = msg.getContent();

                if (historicalInput.contains("你是一个")) {
                    continue;
                }

                log.debug("【恢复上下文】分析历史消息：{}", historicalInput);

                Set<String> cities = extractCities(historicalInput);
                if (!cities.isEmpty()) {
                    assignCities(context, cities, historicalInput);
                }

                LocalDate date = extractDateSmart(historicalInput);
                if (date != null && context.getCollectedParam("departureDate") == null) {
                    context.addCollectedParam("departureDate", date.toString());
                    log.info("【恢复上下文】从历史消息中提取到日期：{}", date);
                }

                Double budget = extractBudget(historicalInput);
                if (budget != null && context.getCollectedParam("budget") == null) {
                    context.addCollectedParam("budget", budget);
                    log.info("【恢复上下文】从历史消息中提取到预算：{}", budget);
                }

                Scene scene = detectSceneFromInput(historicalInput);
                if (scene != null && context.getScene() == null) {
                    context.setScene(scene);
                    context.addCollectedParam("orderType", scene.getValue());
                    log.info("【恢复上下文】从历史消息中提取到订单类型：{}", scene);
                }
            }
        }

        log.info("【恢复上下文】完成，当前已收集参数：{}", context.getCollectedParams());
    }

    private Double extractBudget(String input) {
        log.debug("【预算提取】输入：{}", input);

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:预算|元|以内|不超过|<=|小于)(\\d+(?:\\.\\d+)?)");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            Double budget = Double.parseDouble(matcher.group(1));
            log.debug("【预算提取】匹配到预算金额：{}", budget);
            return budget;
        }

        if (input.contains("预算") || input.contains("元")) {
            java.util.regex.Pattern simplePattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)");
            java.util.regex.Matcher simpleMatcher = simplePattern.matcher(input);
            if (simpleMatcher.find()) {
                try {
                    Double budget = Double.parseDouble(simpleMatcher.group(1));
                    if (budget > 100 && budget < 100000) {
                        log.debug("【预算提取】匹配到简单数字预算：{}", budget);
                        return budget;
                    }
                } catch (NumberFormatException e) {
                    log.debug("【预算提取】数字解析失败：{}", simpleMatcher.group(1));
                }
            }
        }

        log.debug("【预算提取】未匹配到预算");
        return null;
    }

    private IntentRecognitionResult recognizeIntentByLLM(DialogContext context, String userInput,
                                                         List<ChatMessageVO> history) {
        try {
            // 使用多意图识别
            List<Intent> intents = multiIntentHandler.parseMultiIntent(userInput, context.getSessionId().toString());

            if (!intents.isEmpty()) {
                // 取最高优先级的意图
                Intent primaryIntent = intents.get(0);
                log.info("【多意图识别】主意图：{}, 置信度：{}",
                        primaryIntent.getIntentName(), primaryIntent.getConfidence());

                // 处理所有识别到的意图
                multiIntentHandler.handleMultiIntent(intents, context);

                return new IntentRecognitionResult(
                        primaryIntent.getIntentType(),
                        primaryIntent.getConfidence(),
                        primaryIntent.getReason()
                );
            }

            // 降级到单意图识别
            return fallbackToSingleIntent(context, userInput);

        } catch (Exception e) {
            log.warn("多意图识别失败，降级到规则匹配", e);
            IntentType fallbackIntent = Clarify.calarifyIntent(userInput);
            return new IntentRecognitionResult(fallbackIntent, 0.5);
        }
    }

    private IntentRecognitionResult fallbackToSingleIntent(DialogContext context, String userInput) {
        try {
            String intentPrompt;

            if (context.getScene() != null && sceneHandlerFactory.isSupported(context.getScene())) {
                SceneHandler handler = sceneHandlerFactory.getHandler(context.getScene());
                intentPrompt = handler.getIntentRecognitionPrompt(context, userInput);
            } else {
                intentPrompt = buildGenericIntentRecognitionPrompt(context, userInput);
            }

            String response = chatClient.prompt()
                    .system(intentPrompt)
                    .user(userInput)
                    .advisors(advisor -> advisor.param("chatMemoryContextId", "intent_" + context.getSessionId()))
                    .call()
                    .content();

            log.info("【单意图识别LLM响应】原始响应：{}", response);

            return parseIntentRecognitionResult(response);

        } catch (Exception e) {
            log.warn("单意图识别失败，降级到规则匹配", e);
            IntentType fallbackIntent = Clarify.calarifyIntent(userInput);
            return new IntentRecognitionResult(fallbackIntent, 0.5);
        }
    }

    private String buildGenericIntentRecognitionPrompt(DialogContext context, String userInput) {
        PromptTemplate template = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/intent-recognition.st")
        );

        Map<String, Object> model = Map.of(
                "stage", context.getCurrentStage() != null ? context.getCurrentStage().getDesc() : "空闲",
                "params", context.getCollectedParams().keySet(),
                "input", userInput
        );

        return template.render(model);
    }

    private IntentRecognitionResult parseIntentRecognitionResult(String llmResponse) {
        try {
            String jsonStr = extractJsonFromResponse(llmResponse);
            log.debug("【意图识别】提取的JSON：{}", jsonStr);

            JSONObject json = JSONObject.parseObject(jsonStr);

            String intentStr = json.getString("intent");
            double confidence = json.getDoubleValue("confidence");
            String reason = json.getString("reason");

            if (intentStr == null || intentStr.isEmpty()) {
                log.warn("【意图识别】LLM返回的intent为空，使用规则匹配");
                return fallbackToRuleMatching(llmResponse);
            }

            IntentType intentType = IntentType.valueOf(intentStr.toUpperCase());

            log.debug("意图识别解析：intent={}, confidence={}, reason={}",
                    intentType, confidence, reason);

            return new IntentRecognitionResult(intentType, confidence, reason);

        } catch (Exception e) {
            log.warn("解析意图识别结果失败，使用规则匹配。原始响应：{}", llmResponse, e);
            return fallbackToRuleMatching(llmResponse);
        }
    }

    private IntentRecognitionResult parseIntentRecognitionResultWithStructuredOutput(String userInput, DialogContext context) {
        try {
            String intentPrompt = buildGenericIntentRecognitionPrompt(context, userInput);

            LlmIntentResult result = chatClient.prompt()
                    .system(intentPrompt)
                    .user(userInput)
                    .advisors(advisor -> advisor.param("chatMemoryContextId", "intent_" + context.getSessionId()))
                    .call()
                    .entity(LlmIntentResult.class);

            if (result == null || result.getIntent() == null) {
                log.warn("【结构化输出】LLM返回结果为空");
                return fallbackToRuleMatching("");
            }

            IntentType intentType = IntentType.valueOf(result.getIntent().toUpperCase());
            return new IntentRecognitionResult(intentType, result.getConfidence(), result.getReason());

        } catch (Exception e) {
            log.warn("结构化输出失败，降级到手工解析", e);
            return parseIntentRecognitionResult(chatClient.prompt()
                    .user(userInput)
                    .call()
                    .content());
        }
    }

    private IntentRecognitionResult fallbackToRuleMatching(String llmResponse) {
        if (llmResponse.contains("BOOKING")) {
            return new IntentRecognitionResult(IntentType.BOOKING, 0.7);
        } else if (llmResponse.contains("PROVIDING_INFO")) {
            return new IntentRecognitionResult(IntentType.PROVIDING_INFO, 0.7);
        } else if (llmResponse.contains("CONSULTATION")) {
            return new IntentRecognitionResult(IntentType.CONSULTATION, 0.7);
        } else if (llmResponse.contains("CHITCHAT")) {
            return new IntentRecognitionResult(IntentType.CHITCHAT, 0.7);
        }
        return new IntentRecognitionResult(IntentType.OTHER, 0.5);
    }


    private String extractJsonFromResponse(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return response;
    }

    private boolean isInBusinessFlow(DialogStage stage) {
        return stage != null && stage != DialogStage.IDLE &&
                stage != DialogStage.INTENT_RECOGNITION &&
                stage != DialogStage.CONSULTATION;
    }

    private OrderGenerationResponse handleBookingIntent(DialogContext context, String userInput,
                                                        List<ChatMessageVO> history, RagPolicyResult ragResult) {
        if (isInBusinessFlow(context.getCurrentStage())) {
            return processByStage(context, userInput, history, ragResult);
        }

        Scene detectedScene = detectSceneFromInput(userInput);
        if (detectedScene != null && context.getScene() == null) {
            context.setScene(detectedScene);
            log.info("【预订意图】检测到场景：{}", detectedScene);
        }

        context.advanceStage(DialogStage.TRIP_APPLICATION);
        return handleTripApplicationStage(context, userInput, history);
    }

    private OrderGenerationResponse handleChitChatDuringBusiness(DialogContext context, String userInput) {
        log.info("【业务中闲聊】用户输入：{}", userInput);

        String naturalResponse = generateNaturalResponse(userInput);

        OrderGenerationResponse response = new OrderGenerationResponse();
        response.setSessionId(context.getSessionId());
        response.setStatus("complete");
        response.setMessage(naturalResponse + "\n\n" + getBusinessReminder(context));

        return response;
    }

    private String generateNaturalResponse(String userInput) {
        try {
            String prompt = "你是一个友好、幽默的智能商旅助手。请用自然的口语化中文回答用户的问题，不要超过 50 字。";

            return chatClient.prompt()
                    .system(prompt)
                    .user(userInput)
                    .advisors(advisor -> advisor.param("chatMemoryContextId", "chitchat_" + System.currentTimeMillis()))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("通义千问调用失败，使用预设回复", e);
            return getDefaultChitChatResponse(userInput);
        }
    }

    private String getBusinessReminder(DialogContext context) {
        DialogStage stage = context.getCurrentStage();
        if (stage == null) {
            return "";
        }

        switch (stage) {
            case PARAMETER_COLLECTION:
                return "😊 我们还在为您预订机票，请告诉我您的出发城市是？";
            case SOLUTION_RECOMMENDATION:
                return "😊 已为您找到几个方案，请选择一个继续；";
            case FEATURE_SELECTION:
                return "😊 请选择您需要的舱位等级哦~";
            case COMPLIANCE_CHECK:
                return "😊 请确认订单信息，回复'确认下单'即可";
            default:
                return "😊 请问还需要帮您什么？";
        }
    }

    private OrderGenerationResponse handleChitChat(DialogContext context, String userInput) {
        log.info("【闲聊】用户输入：{}", userInput);

        String naturalResponse = generateNaturalResponse(userInput);

        OrderGenerationResponse response = new OrderGenerationResponse();
        response.setStatus("complete");
        response.setMessage(naturalResponse);
        response.setSessionId(context.getSessionId());

        return response;
    }

    private String getDefaultChitChatResponse(String userInput) {
        if (userInput.contains("天气")) {
            return "哈哈，我暂时还不会看天气呢~ 不过我可以帮您预订出差行程，这个我可专业了！☀️";
        } else if (userInput.contains("你好") || userInput.contains("您好")) {
            return "您好呀！我是您的智能商旅助手，订机票、酒店、火车票都找我哦~ 😊";
        } else if (userInput.contains("谢谢")) {
            return "不客气呢！有需要随时找我~ 😄";
        } else if (userInput.contains("是谁") || userInput.contains("身份")) {
            return "我是您的智能商旅助手，专门帮您处理差旅预订，让出差变得更简单~ ✨";
        }
        return "嗯嗯，我在听呢！如果需要预订差旅服务，随时告诉我哦~ 😊";
    }

    private OrderGenerationResponse processByStage(DialogContext context, String userInput,
                                                   List<ChatMessageVO> history, RagPolicyResult ragResult) {
        switch (context.getCurrentStage()) {
            case TRIP_APPLICATION:
                return handleTripApplicationStage(context, userInput, history);
            case PARAMETER_COLLECTION:
                return handleParameterCollectionStage(context, userInput, history, ragResult);
            case SOLUTION_RECOMMENDATION:
                return solutionService.handleSolutionRecommendationStage(context, userInput, history, ragResult);
            case FEATURE_SELECTION:
                return handleFeatureSelectionStage(context, userInput, history, ragResult);
            case COMPLIANCE_CHECK:
                return handleComplianceCheckStage(context, userInput, history, ragResult);
            case FINAL_CONFIRMATION:
                return handleFinalConfirmationStage(context);
            case ORDER_COMPLETED:
                return handleOrderCompletedStage(context, userInput, history);
            case CONSULTATION:
                return handleConsultationStage(context, userInput, history, ragResult);
            default:
                return handleIntentRecognition(context, userInput, history, ragResult);
        }
    }

    private OrderGenerationResponse handleIntentRecognition(DialogContext context, String userInput,
                                                            List<ChatMessageVO> history, RagPolicyResult ragResult) {
        log.info("【意图识别】用户输入：{}", userInput);

        IntentType intentType = Clarify.calarifyIntent(userInput);

        OrderGenerationResponse response = new OrderGenerationResponse();
        switch (intentType) {
            case BOOKING:
                context.advanceStage(DialogStage.TRIP_APPLICATION);
                response = handleTripApplicationStage(context, userInput, history);
                break;
            case CONSULTATION:
                context.advanceStage(DialogStage.CONSULTATION);
                response = handleConsultationStage(context, userInput, history, ragResult);
                break;
            case CHITCHAT:
                response = handleChitChat(context, userInput);
                break;
            case SOLUTION_RECOMMENDATION:
                response = solutionService.handleSolutionRecommendationStage(context, userInput, history, ragResult);
                break;
            case OTHER:
                response = handleUnknownIntent(context, userInput);
        }

        response.setSessionId(context.getSessionId());
        return response;
    }

    private OrderGenerationResponse handleConsultationStage(DialogContext context, String userInput,
                                                            List<ChatMessageVO> history, RagPolicyResult ragResult) {
        log.info("【咨询问答】用户问题：{}", userInput);

        OrderGenerationResponse response = new OrderGenerationResponse();
        response.setSessionId(context.getSessionId());

        if (ragResult.hasPolicy) {
            response.setStatus("complete");
            response.setMessage("根据企业差旅政策：\n\n" + ragResult.getPolicyText() +
                    "\n\n还有其他问题吗？如需预订请告诉我。");
        } else {
            response.setStatus("clarify");
            response.setMessage("抱歉，我没有找到相关政策信息。您可以询问差旅标准、报销流程等问题。");
        }

        context.advanceStage(DialogStage.IDLE);
        return response;
    }


    private OrderGenerationResponse handleTripApplicationStage(DialogContext context, String userInput, List<ChatMessageVO> history) {
        log.info("【出差申请】用户输入：{}", userInput);

        Scene detectedScene = detectSceneFromInput(userInput);

        if (detectedScene != null) {
            context.setScene(detectedScene);
            context.addCollectedParam("orderType", detectedScene.getValue());

            extractEntitiesFromInput(context, userInput);

            SceneHandler handler = sceneHandlerFactory.getHandler(detectedScene);
            List<String> missingParams = handler.getRequiredParams();
            StringBuilder questionBuilder = new StringBuilder();

            for (String param : missingParams) {
                Object value = context.getCollectedParam(param);
                if (value == null || "".equals(value.toString().trim())) {
                    questionBuilder.append(Clarify.getClarificationQuestion(param)).append(" ");
                }
            }

            OrderGenerationResponse response = new OrderGenerationResponse();
            response.setSessionId(context.getSessionId());

            if (questionBuilder.length() > 0) {
                response.setStatus("clarify");
                response.setMessage(questionBuilder.toString().trim());
            } else {
                response.setStatus("clarify");
                response.setMessage(String.format("好的，帮您预订%s。请问：%s",
                        detectedScene.getDesc(),
                        getInitialQuestions(detectedScene)));
            }

            context.advanceStage(DialogStage.PARAMETER_COLLECTION);
            return response;
        } else {
            OrderGenerationResponse response = new OrderGenerationResponse();
            response.setSessionId(context.getSessionId());
            response.setStatus("clarify");
            response.setMessage("抱歉，我没有理解您要预订什么。请问是机票、酒店、火车票还是用车呢？");
            return response;
        }
    }

    private OrderGenerationResponse handleParameterCollectionStage(DialogContext context, String userInput,
                                                                   List<ChatMessageVO> history, RagPolicyResult ragResult) {
        SceneHandler handler = sceneHandlerFactory.getHandler(context.getScene());
        return handler.handleParameterCollection(context, userInput, history, ragResult);
    }

    private OrderGenerationResponse handleFeatureSelectionStage(DialogContext context, String userInput,
                                                                List<ChatMessageVO> history, RagPolicyResult ragResult) {
        log.info("【特征选择】用户选择：{}", userInput);

        String featureLevel = "经济型";
        if (userInput.contains("商务")) {
            featureLevel = "商务型";
        } else if (userInput.contains("豪华")) {
            featureLevel = "豪华型";
        }
        context.addCollectedParam("featureLevel", featureLevel);

        OrderGenerationResponse response = new OrderGenerationResponse();
        response.setSessionId(context.getSessionId());
        response.setStatus("confirm");
        response.setMessage("请确认订单信息：\n" + buildOrderSummary(context) +
                "\n\n确认无误请回复<确认下单> 4 个字即可'");

        context.advanceStage(DialogStage.COMPLIANCE_CHECK);
        return response;
    }

    private OrderGenerationResponse handleComplianceCheckStage(DialogContext context, String userInput,
                                                               List<ChatMessageVO> history, RagPolicyResult ragResult) {
        log.info("【合规校验】开始差标和政策校验");

        if (!userInput.contains("确认")
                && !userInput.contains("确定")
                && !userInput.contains("下单")
                && !userInput.contains("出单")
                && !userInput.contains("好的")
                && !userInput.contains("没问题")) {
            OrderGenerationResponse response = new OrderGenerationResponse();
            response.setSessionId(context.getSessionId());
            response.setStatus("clarify");
            response.setMessage("请回复<确认下单>4 字以继续（支持：确认/确定/下单/出单/好的/没问题）");
            return response;
        }

        OrderRequirements order = buildOrderFromContext(context);
        context.setOrderRequirements(order);

        String policyViolation = validateOrderByRagPolicy(order, ragResult);
        if (policyViolation != null) {
            OrderGenerationResponse response = new OrderGenerationResponse();
            response.setSessionId(context.getSessionId());
            response.setStatus("violation");
            response.setViolationReason(policyViolation);
            response.setMessage("违反差旅政策：" + policyViolation);
            context.advanceStage(DialogStage.SOLUTION_RECOMMENDATION);
            return response;
        }

        ComplianceResult standardResult = complianceService.validateByStandard(order, context.getCompanyId());
        if (!standardResult.getCompliant()) {
            OrderGenerationResponse response = new OrderGenerationResponse();
            response.setSessionId(context.getSessionId());
            response.setStatus("violation");
            response.setViolationReason(standardResult.getViolationReason());
            response.setMessage("违反差标规定：" + standardResult.getViolationReason() +
                    "\n是否需要调整预算或选择其他方案？");
            context.advanceStage(DialogStage.SOLUTION_RECOMMENDATION);
            return response;
        }

        context.setComplianceChecked(true);
        context.setComplianceResult(true);

        context.advanceStage(DialogStage.FINAL_CONFIRMATION);

        return handleFinalConfirmationStage(context);
    }

    private OrderGenerationResponse handleFinalConfirmationStage(DialogContext context) {
        log.info("【最终确认】调用订单生成接口");
        if (ObjectUtils.isEmpty(context.getOrderRequirements())) {
            OrderRequirements order = buildOrderFromContext(context);
            context.setOrderRequirements(order);
        }
        Response<OrderGenerationResponse> feignResponse = orderFeignClient.createOrder(context.getOrderRequirements());

        if (feignResponse.isSuccess() && feignResponse.getData() != null &&
                feignResponse.getData().getOrder() != null) {
            OrderGenerationResponse response = new OrderGenerationResponse();
            response.setSessionId(context.getSessionId());
            response.setStatus("complete");
            response.setOrder(feignResponse.getData().getOrder());
            response.setMessage("订单符合差标和政策规定，已生成平台订单，以最终渠道为准！\n订单号：" + feignResponse.getData().getOrder().getOrderNo() +
                    "\n" + buildOrderSummary(context));

            context.advanceStage(DialogStage.ORDER_COMPLETED);
            return response;
        } else {
            OrderGenerationResponse response = new OrderGenerationResponse();
            response.setSessionId(context.getSessionId());
            response.setStatus("error");
            response.setMessage("订单生成失败：" + (feignResponse != null ? feignResponse.getMessage() : "未知错误"));
            context.advanceStage(DialogStage.IDLE);
            return response;
        }
    }

    private OrderGenerationResponse handleOrderCompletedStage(DialogContext context, String userInput, List<ChatMessageVO> history) {
        log.info("【下单完成】");

        OrderGenerationResponse response = new OrderGenerationResponse();
        response.setSessionId(context.getSessionId());
        response.setStatus("complete");

        if (userInput.contains("新") || userInput.contains("重新") || userInput.contains("再")) {
            context.reset();
            response.setMessage("订单已完成！如需新的预订请告诉我您的需求。");
        } else {
            response.setMessage("订单已完成！如有其他需求请随时告诉我。");
        }

        context.advanceStage(DialogStage.IDLE);
        return response;
    }

    private Scene detectSceneFromInput(String input) {
        if (input.contains("机票") || input.contains("航班") || input.contains("飞机")) {
            return Scene.FLIGHT;
        } else if (input.contains("酒店") || input.contains("住宿") || input.contains("宾馆")) {
            return Scene.HOTEL;
        } else if (input.contains("火车") || input.contains("高铁") || input.contains("动车")) {
            return Scene.TRAIN;
        } else if (input.contains("车") || input.contains("用车") || input.contains("打车")) {
            return Scene.CAR;
        }
        return null;
    }

    private String getInitialQuestions(Scene scene) {
        if (sceneHandlerFactory.isSupported(scene)) {
            SceneHandler handler = sceneHandlerFactory.getHandler(scene);
            return handler.getInitialQuestion(null);
        }
        switch (scene) {
            case FLIGHT:
                return "出发城市、到达城市、出发日期是？";
            case HOTEL:
                return "入住城市、入住日期、离店日期是？";
            case TRAIN:
                return "出发城市、到达城市、出发日期是？";
            case CAR:
                return "用车城市、用车时间、目的地是？";
            default:
                return "请提供详细信息";
        }
    }

    private void extractEntitiesFromInput(DialogContext context, String input) {
        log.info("【实体提取】输入：{}", input);

        String normalizedInput = input.replaceAll("[\\s,，,.]+", " ");

        Set<String> cities = extractCities(normalizedInput);
        if (!cities.isEmpty()) {
            assignCities(context, cities, normalizedInput);
        }

        LocalDate date = extractDateSmart(normalizedInput);
        if (date != null) {
            context.addCollectedParam("departureDate", date.toString());
            log.info("【实体提取】提取到日期：{}", date);
        }

        Double budget = extractBudget(normalizedInput);
        if (budget != null) {
            context.addCollectedParam("budget", budget);
            log.info("【实体提取】提取到预算：{}", budget);
        }

        if (context.getScene() == null) {
            Scene scene = detectSceneFromInput(input);
            if (scene != null) {
                context.setScene(scene);
                context.addCollectedParam("orderType", scene.getValue());
                log.info("【实体提取】提取到订单类型：{}", scene);
            }
        }

        log.info("【实体提取】完成，当前收集的参数：{}", context.getCollectedParams());
    }

    private Set<String> extractCities(String input) {
        Set<String> cities = new HashSet<>();
        String[] cityList = {"北京", "上海", "广州", "深圳", "成都", "杭州",
                "南京", "重庆", "武汉", "西安", "苏州", "天津"};

        for (String city : cityList) {
            if (input.contains(city)) {
                cities.add(city);
            }
        }

        return cities;
    }

    private void assignCities(DialogContext context, Set<String> cities, String input) {
        List<String> cityList = new ArrayList<>(cities);

        if (cityList.size() == 1) {
            String city = cityList.get(0);
            Object existingDeparture = context.getCollectedParam("departureCity");
            Object existingArrival = context.getCollectedParam("arrivalCity");

            if (existingDeparture == null && existingArrival == null) {
                context.addCollectedParam("departureCity", city);
                log.info("【城市分配】单一城市 {} 作为出发地", city);
            } else if (existingDeparture != null && existingArrival == null) {
                context.addCollectedParam("arrivalCity", city);
                log.info("【城市分配】单一城市 {} 作为目的地", city);
            }
            return;
        }

        if (cityList.size() >= 2) {
            boolean hasFromKeyword = input.contains("从") ||
                    input.contains("出发") ||
                    input.contains("离开");
            boolean hasToKeyword = input.contains("到") ||
                    input.contains("去") ||
                    input.contains("前往") ||
                    input.contains("达");

            if (hasFromKeyword && !hasToKeyword) {
                context.addCollectedParam("departureCity", cityList.get(0));
                context.addCollectedParam("arrivalCity", cityList.get(1));
            } else if (!hasFromKeyword && hasToKeyword) {
                context.addCollectedParam("arrivalCity", cityList.get(0));
                context.addCollectedParam("departureCity", cityList.get(1));
            } else {
                context.addCollectedParam("departureCity", cityList.get(0));
                context.addCollectedParam("arrivalCity", cityList.get(1));
            }

            log.info("【城市分配】出发地={}, 目的地={}",
                    context.getCollectedParam("departureCity"),
                    context.getCollectedParam("arrivalCity"));
        }
    }

    private LocalDate extractDateSmart(String input) {
        log.debug("【日期提取】输入：{}", input);

        if (input.contains("今天") || input.contains("当日")) {
            log.debug("【日期提取】匹配到'今天'");
            return LocalDate.now();
        }
        if (input.contains("明天") || input.contains("次日")) {
            log.debug("【日期提取】匹配到'明天'");
            return LocalDate.now().plusDays(1);
        }
        if (input.contains("后天")) {
            log.debug("【日期提取】匹配到'后天'");
            return LocalDate.now().plusDays(2);
        }
        if (input.contains("大后天")) {
            log.debug("【日期提取】匹配到'大后天'");
            return LocalDate.now().plusDays(3);
        }

        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日");
        java.util.regex.Matcher matcher1 = pattern1.matcher(input);
        if (matcher1.find()) {
            int year = Integer.parseInt(matcher1.group(1));
            int month = Integer.parseInt(matcher1.group(2));
            int day = Integer.parseInt(matcher1.group(3));
            log.debug("【日期提取】匹配到年月日格式：{}-{}-{}", year, month, day);
            return LocalDate.of(year, month, day);
        }

        java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
        java.util.regex.Matcher matcher2 = pattern2.matcher(input);
        if (matcher2.find()) {
            int year = Integer.parseInt(matcher2.group(1));
            int month = Integer.parseInt(matcher2.group(2));
            int day = Integer.parseInt(matcher2.group(3));
            log.debug("【日期提取】匹配到短横线格式：{}-{}-{}", year, month, day);
            return LocalDate.of(year, month, day);
        }

        java.util.regex.Pattern pattern3 = java.util.regex.Pattern.compile("(\\d{1,2})月(\\d{1,2})日");
        java.util.regex.Matcher matcher3 = pattern3.matcher(input);
        if (matcher3.find()) {
            int month = Integer.parseInt(matcher3.group(1));
            int day = Integer.parseInt(matcher3.group(2));
            int year = LocalDate.now().getYear();
            LocalDate date = LocalDate.of(year, month, day);
            if (date.isBefore(LocalDate.now())) {
                date = date.plusYears(1);
            }
            log.debug("【日期提取】匹配到月日格式：{}-{}-{}", year, month, day);
            return date;
        }

        log.debug("【日期提取】未匹配到日期");
        return null;
    }


    private String buildOrderSummary(DialogContext context) {
        StringBuilder summary = new StringBuilder();
        summary.append("订单类型：").append(context.getScene().getDesc()).append("\n");

        Object departure = context.getCollectedParam("departureCity");
        if (departure != null) {
            summary.append("出发地：").append(departure).append("\n");
        }

        Object arrival = context.getCollectedParam("arrivalCity");
        if (arrival != null) {
            summary.append("目的地：").append(arrival).append("\n");
        }

        Object date = context.getCollectedParam("departureDate");
        if (date != null) {
            summary.append("日期：").append(date).append("\n");
        }

        Object feature = context.getCollectedParam("featureLevel");
        if (feature != null) {
            summary.append("等级：").append(feature).append("\n");
        }

        if (null != context.getSelectedSolutionIndex() && context.getRecommendedSolutions().size() > 0) {
            Object selectedSolution = context.getRecommendedSolutions().get(context.getSelectedSolutionIndex());
            JSONObject solution = JSONObject.parseObject(JSONObject.toJSONString(selectedSolution));
            summary.append("方案：").append(solution.getString("solutionName")).append("\n");
        }

        return summary.toString();
    }

    private OrderRequirements buildOrderFromContext(DialogContext context) {
        OrderRequirements order = new OrderRequirements();
        order.setOrderType(context.getScene().getValue());

        Object selectedSolution = context.getCollectedParam("selectedSolution");
        if (selectedSolution != null) {
            try {
                Map<String, Object> solutionMap;
                if (selectedSolution instanceof Map) {
                    solutionMap = (Map<String, Object>) selectedSolution;
                } else {
                    solutionMap = JSONObject.parseObject(JSONObject.toJSONString(selectedSolution), Map.class);
                }

                Object priceObj = solutionMap.get("price");
                if (priceObj != null) {
                    Double price;
                    if (priceObj instanceof Number) {
                        price = ((Number) priceObj).doubleValue();
                    } else {
                        price = Double.parseDouble(priceObj.toString());
                    }
                    order.setBudget(price);
                    log.info("【构建订单】从选中方案提取预算：{}", price);
                }

                if ("FLIGHT".equals(context.getScene().getValue())) {
                    Object flightNo = solutionMap.get("flightNo");
                    Object airline = solutionMap.get("airline");
                    Object cabinClass = solutionMap.get("cabinClass");

                    if (flightNo != null) {
                        order.setFlightNo(flightNo.toString());
                    }
                    if (airline != null) {
                        order.setAirline(airline.toString());
                    }
                    if (cabinClass != null) {
                        order.setCabinClass(cabinClass.toString());
                    }
                }
            } catch (Exception e) {
                log.warn("解析选中方案失败：{}", selectedSolution, e);
            }
        }

        if (order.getBudget() == null) {
            Object budget = context.getCollectedParam("budget");
            if (budget != null) {
                try {
                    if (budget instanceof Double) {
                        order.setBudget((Double) budget);
                    } else if (budget instanceof Number) {
                        order.setBudget(((Number) budget).doubleValue());
                    } else if (budget instanceof String) {
                        order.setBudget(Double.parseDouble((String) budget));
                    }
                } catch (Exception e) {
                    log.warn("预算类型转换失败：{}", budget, e);
                }
            }
        }

        order.setDepartureCity((String) context.getCollectedParam("departureCity"));
        order.setArrivalCity((String) context.getCollectedParam("arrivalCity"));
        order.setDepartureDate((String) context.getCollectedParam("departureDate"));
        order.setArrivalDate((String) context.getCollectedParam("arrivalDate"));

        Object passengers = context.getCollectedParam("passengers");
        if (passengers != null) {
            try {
                List<com.boonya.business.trip.common.entity.Passenger> passengerList =
                        JSONObject.parseArray(JSONObject.toJSONString(passengers),
                                com.boonya.business.trip.common.entity.Passenger.class);
                order.setPassengers(passengerList);
            } catch (Exception e) {
                log.warn("解析乘客信息失败：{}", passengers, e);
            }
        }

        Object specialReqs = context.getCollectedParam("specialRequirements");
        if (specialReqs != null) {
            order.setSpecialRequirements(specialReqs.toString());
        }

        log.info("【构建订单】最终订单参数：{}", order);
        return order;
    }

    private String extractDate(String input) {
        try {
            if (input.matches(".*\\d{4}年\\d{1,2}月\\d{1,2}日.*")) {
                return LocalDate.now().toString();
            }
        } catch (Exception e) {
            log.warn("日期提取失败", e);
        }
        return LocalDate.now().toString();
    }

    private RagPolicyResult callRagPolicy(Scene scene, String userInput, Long sessionId) {
        RagPolicyResult result = new RagPolicyResult();
        try {
            PolicyDocumentQueryRequest queryRequest = new PolicyDocumentQueryRequest();
            queryRequest.setScene(scene);
            queryRequest.setQuery(userInput);

            Response<List<PolicyDocument>> response = policyDocumentFeignClient.searchPolicies(queryRequest);

            if (response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                List<PolicyDocument> policies = response.getData();
                StringBuilder stringBuilder = new StringBuilder();
                for (PolicyDocument policy : policies) {
                    stringBuilder.append(policy.getContent()).append("\n\n");
                }
                result.setPolicyText(stringBuilder.toString());
                result.setHasPolicy(true);
            }
        } catch (Exception e) {
            log.warn("RAG 政策检索失败", e);
            result.setPolicyText("默认差标：机票经济舱≤1500 元，酒店≤400 元/天");
        }
        return result;
    }

    private String validateOrderByRagPolicy(OrderRequirements order, RagPolicyResult ragResult) {
        if (order == null) {
            return "订单数据为空";
        }
        Long companyId = UserHolder.get() != null ? UserHolder.get().getCompanyId() : null;
        if (!ragResult.hasPolicy) {
            return isOrderCompliant(order, companyId) ? null : "违反默认差标规则";
        }
        return isOrderCompliant(order, companyId) ? null : "违反差旅标准";
    }

    /**
     * 差标合规校验：动态从 standards 表读取对应公司的差标上限，
     * 支持 flight / hotel / train / car 四种类型，无法查到时降级为默认值。
     */
    private boolean isOrderCompliant(OrderRequirements order, Long companyId) {
        if (order == null) {
            return false;
        }
        if (order.getBudget() == null) {
            // 未填写预算，视为合规（后续可由审批流二次把关）
            return true;
        }

        double budget = order.getBudget();
        String orderType = order.getOrderType();

        try {
            Standard standard = standardService.getStandard(companyId);
            if ("flight".equalsIgnoreCase(orderType)) {
                Double maxPrice = standard.getFlightMaxPrice();
                log.debug("机票差标校验：budget={}, maxPrice={}", budget, maxPrice);
                return maxPrice == null || budget <= maxPrice;
            } else if ("hotel".equalsIgnoreCase(orderType)) {
                Double maxPrice = standard.getHotelMaxPrice();
                log.debug("酒店差标校验：budget={}, maxPrice={}", budget, maxPrice);
                return maxPrice == null || budget <= maxPrice;
            } else if ("train".equalsIgnoreCase(orderType)) {
                Double maxPrice = standard.getTrainMaxPrice();
                log.debug("火车差标校验：budget={}, maxPrice={}", budget, maxPrice);
                return maxPrice == null || budget <= maxPrice;
            } else if ("car".equalsIgnoreCase(orderType)) {
                Double maxPrice = standard.getCarMaxPrice();
                log.debug("用车差标校验：budget={}, maxPrice={}", budget, maxPrice);
                return maxPrice == null || budget <= maxPrice;
            }
        } catch (Exception e) {
            log.warn("差标查询失败，降级为默认规则校验，companyId={}", companyId, e);
            // 降级：机票 1500 / 酒店 500 / 火车 600 / 用车 200
            if ("flight".equalsIgnoreCase(orderType)) return budget <= 1500;
            if ("hotel".equalsIgnoreCase(orderType))  return budget <= 500;
            if ("train".equalsIgnoreCase(orderType))  return budget <= 600;
            if ("car".equalsIgnoreCase(orderType))    return budget <= 200;
        }

        // 其他类型默认合规
        return true;
    }

    private OrderGenerationResponse handleUnknownIntent(DialogContext context, String userInput) {
        log.info("【未知意图】用户输入：{}", userInput);

        OrderGenerationResponse response = new OrderGenerationResponse();
        response.setSessionId(context.getSessionId());
        response.setStatus("clarify");
        response.setMessage("抱歉，我没有完全理解您的意思。\n\n我可以帮您：\n1. 预订机票、酒店、火车票、用车\n2. 查询差旅政策\n3. 解答差旅相关问题\n\n请问您需要什么帮助？");

        return response;
    }

    private void saveContext(DialogContext context, String redisKey, Long sessionId,
                             String userInput, OrderGenerationResponse response) {
        List<ChatMessageVO> history = redisTemplate.opsForValue().get(redisKey);
        if (history == null) {
            history = loadHistoryFromDB(sessionId);
        }

        if (!(history instanceof ArrayList)) {
            history = new ArrayList<>(history);
        }

        ChatMessageVO userMsg = ChatMessageVO.builder()
                .role("USER")
                .content(userInput)
                .build();
        history.add(userMsg);

        String assistantContent = response.getMessage() != null ? response.getMessage() :
                JSONObject.toJSONString(response);
        ChatMessageVO assistantMsg = ChatMessageVO.builder()
                .role("ASSISTANT")
                .content(assistantContent)
                .build();
        history.add(assistantMsg);

        redisTemplate.opsForValue().set(redisKey, history, SESSION_TTL);
        saveToDB(context.getScene(), sessionId, userMsg, assistantMsg, response);

        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setCurrentStage(context.getCurrentStage().getCode());
            session.setLastActiveTime(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }

    private List<ChatMessageVO> loadHistoryFromDB(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getDeleted, 0)
                .orderByDesc(ChatMessage::getCreateTime);

        List<ChatMessage> dbMsgs = chatMessageMapper.selectList(wrapper);
        return dbMsgs.stream().map(msg -> ChatMessageVO.builder()
                .role(msg.getRole())
                .content(msg.getContent())
                .build()).collect(Collectors.toList());
    }

    private Long saveToDB(Scene scene, Long sessionId, ChatMessageVO userMsg, ChatMessageVO assistantMsg, OrderGenerationResponse resp) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            UserHolder userContext = UserHolder.get();
            session = new ChatSession();
            session.setId(sessionId);
            session.setUserId(userContext.getUserId());
            session.setCompanyId(userContext.getCompanyId());
            session.setScene(scene);
            session.setTitle(scene.getDesc() + "-" + LocalDateTime.now());
            session.setStatus(SessionStatus.ACTIVE);
            session.setCurrentStage(DialogStage.IDLE.getCode());
            chatSessionMapper.insert(session);
            sessionId = session.getId();
        } else {
            session.setLastActiveTime(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setRole(userMsg.getRole());
        userMessage.setContent(userMsg.getContent());
        chatMessageMapper.insert(userMessage);

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setRole(assistantMsg.getRole());
        assistantMessage.setContent(assistantMsg.getContent());
        chatMessageMapper.insert(assistantMessage);

        return sessionId;
    }

    public List<ChatMessageVO> getHistory(String sessionIdStr) {
        Long sessionId = Long.parseLong(sessionIdStr);
        String redisKey = SESSION_KEY_PREFIX + sessionId;
        List<ChatMessageVO> history = redisTemplate.opsForValue().get(redisKey);
        if (history != null) {
            return history;
        }
        return loadHistoryFromDB(sessionId);
    }

    public void clearSession(String sessionIdStr) {
        Long sessionId = Long.parseLong(sessionIdStr);
        String redisKey = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.delete(redisKey);

        LambdaUpdateWrapper<ChatSession> sessionWrapper = new LambdaUpdateWrapper<>();
        sessionWrapper.eq(ChatSession::getId, sessionId)
                .set(ChatSession::getDeleted, 1);
        chatSessionMapper.update(sessionWrapper);

        LambdaUpdateWrapper<ChatMessage> msgWrapper = new LambdaUpdateWrapper<>();
        msgWrapper.eq(ChatMessage::getSessionId, sessionId)
                .set(ChatMessage::getDeleted, 1);
        chatMessageMapper.update(msgWrapper);
    }

    /**
     * 大模型意图识别结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LlmIntentResult {
        private String intent;
        private Double confidence;
        private String reason;
    }

    /**
     * 大模型意图识别结果
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class IntentRecognitionResult {
        private IntentType intentType;
        private Double confidence;
        private String reason;

        public IntentRecognitionResult(IntentType intentType, double confidence) {
            this(intentType, confidence, "");
        }
    }

}
