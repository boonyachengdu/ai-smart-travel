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
public class TrainSceneHandler extends AbstractSceneHandler {

    private final SolutionService solutionService;

    private static final Set<String> CITIES = new HashSet<>(Arrays.asList(
            "北京", "上海", "广州", "深圳", "成都", "杭州",
            "南京", "重庆", "武汉", "西安", "苏州", "天津"
    ));

    @Override
    public Scene getSupportedScene() {
        return Scene.TRAIN;
    }

    @Override
    public List<String> getRequiredParams() {
        return Arrays.asList("departureCity", "arrivalCity", "departureDate");
    }

    @Override
    public String getInitialQuestion(DialogContext context) {
        return "出发城市、到达城市、出发日期是？";
    }

    @Override
    public String getOrderPrompt() {
        return ScenePrompt.TRAIN_ORDER_PROMPT;
    }

    @Override
    public String getIntentRecognitionPrompt(DialogContext context, String userInput) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个火车票预订助手，需要识别用户意图并提取火车信息。\n\n");
        prompt.append("当前已收集参数：").append(context.getCollectedParams().keySet()).append("\n");
        prompt.append("用户输入：").append(userInput).append("\n\n");

        prompt.append("请判断用户意图：\n");
        prompt.append("- BOOKING: 预订火车票\n");
        prompt.append("- PROVIDING_INFO: 提供火车相关信息（城市、日期、车次等）\n");
        prompt.append("- CONSULTATION: 咨询火车政策、退改签规则等\n");
        prompt.append("- CHITCHAT: 闲聊\n");
        prompt.append("- OTHER: 其他\n\n");

        prompt.append("如果是 BOOKING 或 PROVIDING_INFO，请提取以下字段（如果存在）：\n");
        prompt.append("- departureCity: 出发城市\n");
        prompt.append("- arrivalCity: 到达城市\n");
        prompt.append("- departureDate: 出发日期（YYYY-MM-DD格式）\n");
        prompt.append("- trainType: 列车类型（HIGH_SPEED/NORMAL/ANY）\n");
        prompt.append("- seatClass: 席别（SECOND_CLASS/FIRST_CLASS/BUSINESS）\n");
        prompt.append("- trainNo: 车次号\n");
        prompt.append("- budget: 预算金额\n\n");

        prompt.append("返回 JSON 格式：{\"intent\": \"意图类型\", \"confidence\": 0.0-1.0}\n");

        return prompt.toString();
    }

    @Override
    protected void extractEntities(DialogContext context, String input) {
        log.debug("【火车实体提取】输入：{}", input);

        Set<String> cities = extractCities(input);
        if (!cities.isEmpty()) {
            assignCities(context, cities, input);
        }

        LocalDate date = extractDate(input);
        if (date != null) {
            context.addCollectedParam("departureDate", date.toString());
            log.debug("【火车实体提取】提取到日期：{}", date);
        }

        Double budget = extractBudget(input);
        if (budget != null) {
            context.addCollectedParam("budget", budget);
            log.debug("【火车实体提取】提取到预算：{}", budget);
        }
    }

    @Override
    public List<?> generateSolutions(DialogContext context, RagPolicyResult ragResult) {
        return solutionService.generateSolutions(context, ragResult);
    }

    @Override
    public String buildOrderSummary(DialogContext context) {
        StringBuilder summary = new StringBuilder();
        summary.append("订单类型：火车票预订\n");

        Object departure = context.getCollectedParam("departureCity");
        if (departure != null) summary.append("出发地：").append(departure).append("\n");

        Object arrival = context.getCollectedParam("arrivalCity");
        if (arrival != null) summary.append("目的地：").append(arrival).append("\n");

        Object date = context.getCollectedParam("departureDate");
        if (date != null) summary.append("日期：").append(date).append("\n");

        Object feature = context.getCollectedParam("featureLevel");
        if (feature != null) summary.append("席别等级：").append(feature).append("\n");

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

    private void assignCities(DialogContext context, Set<String> cities, String input) {
        List<String> cityList = new ArrayList<>(cities);

        if (cityList.size() == 1) {
            String city = cityList.get(0);
            Object existingDeparture = context.getCollectedParam("departureCity");
            Object existingArrival = context.getCollectedParam("arrivalCity");

            if (existingDeparture == null && existingArrival == null) {
                context.addCollectedParam("departureCity", city);
            } else if (existingDeparture != null && existingArrival == null) {
                context.addCollectedParam("arrivalCity", city);
            }
            return;
        }

        if (cityList.size() >= 2) {
            context.addCollectedParam("departureCity", cityList.get(0));
            context.addCollectedParam("arrivalCity", cityList.get(1));
        }
    }

    private LocalDate extractDate(String input) {
        if (input.contains("今天")) return LocalDate.now();
        if (input.contains("明天")) return LocalDate.now().plusDays(1);
        if (input.contains("后天")) return LocalDate.now().plusDays(2);

        Pattern pattern = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
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
