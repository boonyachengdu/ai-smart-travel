package com.boonya.business.trip.dialog.agent.scene.impl;

import com.boonya.business.trip.common.constant.Scene;
import com.boonya.business.trip.common.models.rag.RagPolicyResult;
import com.boonya.business.trip.dialog.context.DialogContext;
import com.boonya.business.trip.dialog.service.SolutionService;
import com.boonya.business.trip.dialog.agent.scene.AbstractSceneHandler;
import com.boonya.business.trip.dialog.agent.prompt.ScenePrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarSceneHandler extends AbstractSceneHandler {

    private final SolutionService solutionService;

    private static final Set<String> CITIES = new HashSet<>(Arrays.asList(
            "北京", "上海", "广州", "深圳", "成都", "杭州",
            "南京", "重庆", "武汉", "西安", "苏州", "天津"
    ));

    @Override
    public Scene getSupportedScene() {
        return Scene.CAR;
    }

    @Override
    public List<String> getRequiredParams() {
        return Arrays.asList("city", "useTime", "destination");
    }

    @Override
    public String getInitialQuestion(DialogContext context) {
        return "用车城市、用车时间、目的地是？";
    }

    @Override
    public String getOrderPrompt() {
        return ScenePrompt.CAR_ORDER_PROMPT;
    }

    @Override
    public String getIntentRecognitionPrompt(DialogContext context, String userInput) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个用车预订助手，需要识别用户意图并提取用车信息。\n\n");
        prompt.append("当前已收集参数：").append(context.getCollectedParams().keySet()).append("\n");
        prompt.append("用户输入：").append(userInput).append("\n\n");

        prompt.append("请判断用户意图：\n");
        prompt.append("- BOOKING: 预订用车\n");
        prompt.append("- PROVIDING_INFO: 提供用车相关信息（城市、时间、目的地等）\n");
        prompt.append("- CONSULTATION: 咨询用车政策、费用标准等\n");
        prompt.append("- CHITCHAT: 闲聊\n");
        prompt.append("- OTHER: 其他\n\n");

        prompt.append("如果是 BOOKING 或 PROVIDING_INFO，请提取以下字段（如果存在）：\n");
        prompt.append("- city: 用车城市\n");
        prompt.append("- pickupAddress: 上车地点\n");
        prompt.append("- destination: 目的地\n");
        prompt.append("- useTime: 用车时间（YYYY-MM-DD HH:mm格式）\n");
        prompt.append("- carType: 车型（ECONOMY/COMFORT/BUSINESS/LUXURY）\n");
        prompt.append("- passengerCount: 乘客人数\n");
        prompt.append("- budget: 预算金额\n\n");

        prompt.append("返回 JSON 格式：{\"intent\": \"意图类型\", \"confidence\": 0.0-1.0}\n");

        return prompt.toString();
    }

    @Override
    protected void extractEntities(DialogContext context, String input) {
        log.debug("【用车实体提取】输入：{}", input);

        Set<String> cities = extractCities(input);
        if (!cities.isEmpty()) {
            String city = cities.iterator().next();
            context.addCollectedParam("city", city);
            log.debug("【用车实体提取】提取到城市：{}", city);
        }

        String useTime = extractUseTime(input);
        if (useTime != null) {
            context.addCollectedParam("useTime", useTime);
            log.debug("【用车实体提取】提取到用车时间：{}", useTime);
        }

        String destination = extractDestination(input);
        if (destination != null) {
            context.addCollectedParam("destination", destination);
            log.debug("【用车实体提取】提取到目的地：{}", destination);
        }

        Double budget = extractBudget(input);
        if (budget != null) {
            context.addCollectedParam("budget", budget);
            log.debug("【用车实体提取】提取到预算：{}", budget);
        }
    }

    @Override
    public List<?> generateSolutions(DialogContext context, RagPolicyResult ragResult) {
        return solutionService.generateSolutions(context, ragResult);
    }

    @Override
    public String buildOrderSummary(DialogContext context) {
        StringBuilder summary = new StringBuilder();
        summary.append("订单类型：用车预订\n");

        Object city = context.getCollectedParam("city");
        if (city != null) summary.append("城市：").append(city).append("\n");

        Object useTime = context.getCollectedParam("useTime");
        if (useTime != null) summary.append("用车时间：").append(useTime).append("\n");

        Object destination = context.getCollectedParam("destination");
        if (destination != null) summary.append("目的地：").append(destination).append("\n");

        Object feature = context.getCollectedParam("featureLevel");
        if (feature != null) summary.append("车型等级：").append(feature).append("\n");

        return summary.toString();
    }

    @Override
    public Map<String, Object> extractSolutionDetails(Object solution) {
        if (solution instanceof Map) {
            return (Map<String, Object>) solution;
        }
        return Collections.emptyMap();
    }

    private Set<String> extractCities(String input) {
        Set<String> cities = new HashSet<>();
        for (String city : CITIES) {
            if (input.contains(city)) {
                cities.add(city);
            }
        }
        return cities;
    }

    private String extractUseTime(String input) {
        Pattern pattern = Pattern.compile("(\\d{1,2}:\\d{2})");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }

        if (input.contains("现在") || input.contains("立即")) {
            return LocalTime.now().toString();
        }

        return null;
    }

    private String extractDestination(String input) {
        if (input.contains("去") || input.contains("到")) {
            int index = input.indexOf("去") != -1 ? input.indexOf("去") : input.indexOf("到");
            String after = input.substring(index + 1).trim();
            if (after.length() > 0 && after.length() < 50) {
                return after;
            }
        }
        return null;
    }

    private Double extractBudget(String input) {
        Pattern pattern = Pattern.compile("(?:预算|元|以内|不超过)(\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return null;
    }
}
