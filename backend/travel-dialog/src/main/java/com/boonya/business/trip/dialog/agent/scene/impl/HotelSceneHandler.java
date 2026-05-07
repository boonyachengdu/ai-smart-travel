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

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotelSceneHandler extends AbstractSceneHandler {

    private final SolutionService solutionService;

    private static final Set<String> CITIES = new HashSet<>(Arrays.asList(
            "北京", "上海", "广州", "深圳", "成都", "杭州",
            "南京", "重庆", "武汉", "西安", "苏州", "天津"
    ));

    @Override
    public Scene getSupportedScene() {
        return Scene.HOTEL;
    }

    @Override
    public List<String> getRequiredParams() {
        return Arrays.asList("checkInCity", "checkInDate", "checkOutDate");
    }

    @Override
    public String getInitialQuestion(DialogContext context) {
        return "入住城市、入住日期、离店日期是？";
    }

    @Override
    public String getOrderPrompt() {
        return ScenePrompt.HOTEL_ORDER_PROMPT;
    }

    @Override
    public String getIntentRecognitionPrompt(DialogContext context, String userInput) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个酒店预订助手，需要识别用户意图并提取酒店信息。\n\n");
        prompt.append("当前已收集参数：").append(context.getCollectedParams().keySet()).append("\n");
        prompt.append("用户输入：").append(userInput).append("\n\n");

        prompt.append("请判断用户意图：\n");
        prompt.append("- BOOKING: 预订酒店\n");
        prompt.append("- PROVIDING_INFO: 提供酒店相关信息（城市、日期、房型等）\n");
        prompt.append("- CONSULTATION: 咨询酒店政策、入住规则等\n");
        prompt.append("- CHITCHAT: 闲聊\n");
        prompt.append("- OTHER: 其他\n\n");

        prompt.append("如果是 BOOKING 或 PROVIDING_INFO，请提取以下字段（如果存在）：\n");
        prompt.append("- checkInCity: 入住城市\n");
        prompt.append("- checkInDate: 入住日期（YYYY-MM-DD格式）\n");
        prompt.append("- checkOutDate: 离店日期（YYYY-MM-DD格式）\n");
        prompt.append("- roomType: 房型（STANDARD/DELUXE/SUITE）\n");
        prompt.append("- rooms: 房间数量\n");
        prompt.append("- budgetPerNight: 每晚预算\n");
        prompt.append("- brand: 酒店品牌偏好\n\n");

        prompt.append("返回 JSON 格式：{\"intent\": \"意图类型\", \"confidence\": 0.0-1.0}\n");

        return prompt.toString();
    }


    @Override
    protected void extractEntities(DialogContext context, String input) {
        log.debug("【酒店实体提取】输入：{}", input);

        Set<String> cities = extractCities(input);
        if (!cities.isEmpty()) {
            String city = cities.iterator().next();
            context.addCollectedParam("checkInCity", city);
            log.debug("【酒店实体提取】提取到城市：{}", city);
        }

        LocalDate checkInDate = extractDate(input, "入住");
        if (checkInDate != null) {
            context.addCollectedParam("checkInDate", checkInDate.toString());
            log.debug("【酒店实体提取】提取到入住日期：{}", checkInDate);
        }

        LocalDate checkOutDate = extractDate(input, "离店");
        if (checkOutDate != null) {
            context.addCollectedParam("checkOutDate", checkOutDate.toString());
            log.debug("【酒店实体提取】提取到离店日期：{}", checkOutDate);
        }

        Double budget = extractBudget(input);
        if (budget != null) {
            context.addCollectedParam("budget", budget);
            log.debug("【酒店实体提取】提取到预算：{}", budget);
        }
    }

    @Override
    public List<?> generateSolutions(DialogContext context, RagPolicyResult ragResult) {
        return solutionService.generateSolutions(context, ragResult);
    }

    @Override
    public String buildOrderSummary(DialogContext context) {
        StringBuilder summary = new StringBuilder();
        summary.append("订单类型：酒店预订\n");

        Object city = context.getCollectedParam("checkInCity");
        if (city != null) summary.append("城市：").append(city).append("\n");

        Object checkIn = context.getCollectedParam("checkInDate");
        if (checkIn != null) summary.append("入住日期：").append(checkIn).append("\n");

        Object checkOut = context.getCollectedParam("checkOutDate");
        if (checkOut != null) summary.append("离店日期：").append(checkOut).append("\n");

        Object feature = context.getCollectedParam("featureLevel");
        if (feature != null) summary.append("房型等级：").append(feature).append("\n");

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

    private LocalDate extractDate(String input, String keyword) {
        if (input.contains(keyword + "今天")) return LocalDate.now();
        if (input.contains(keyword + "明天")) return LocalDate.now().plusDays(1);

        Pattern pattern = Pattern.compile(keyword + "(\\d{4})-(\\d{1,2})-(\\d{1,2})");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return LocalDate.of(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
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
