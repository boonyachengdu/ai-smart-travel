package com.boonya.business.trip.dialog.chat;


import com.boonya.business.trip.common.models.dialog.constant.IntentType;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: Clarify
 * @Description: Clarify 澄清问题
 */
@Slf4j
public class Clarify {

    /**
     * 根据参数名生成澄清问题
     */
    public static String getClarificationQuestion(String paramKey) {
        Map<String, String> questionMap = new HashMap<>();

        questionMap.put("departureCity", "请问您从哪个城市出发？");
        questionMap.put("arrivalCity", "请问您要前往哪个城市？");
        questionMap.put("departureDate", "请问您的出发日期是？");
        questionMap.put("returnDate", "请问您是否需要返程？如果是，返程日期是？");
        questionMap.put("budget", "请问您的预算是多少？");
        questionMap.put("passengers", "请问出行人是谁？（姓名）");

        questionMap.put("checkInCity", "请问入住哪个城市？");
        questionMap.put("checkInDate", "请问入住日期是？");
        questionMap.put("checkOutDate", "请问离店日期是？");
        questionMap.put("brand", "请问您有偏好的酒店品牌吗？");
        questionMap.put("roomNum", "请问需要几个房间？");
        questionMap.put("adultNum", "请问有几位成人入住？");

        questionMap.put("fromStation", "请问从哪个火车站出发？");
        questionMap.put("toStation", "请问到达哪个火车站？");

        questionMap.put("city", "请问在哪个城市用车？");
        questionMap.put("useTime", "请问用车时间是？");
        questionMap.put("destination", "请问目的地是？");
        questionMap.put("carType", "请问需要什么车型？（经济型/舒适型/商务型）");
        questionMap.put("departure", "请问上车地点是？");

        return questionMap.getOrDefault(paramKey, "请补充：" + paramKey);
    }

    public static IntentType calarifyIntent(String input) {
        String lowerInput = input.toLowerCase().trim();

        // 1. 优先判断是否为纯数字（方案选择）
        try {
            Integer num = Integer.parseInt(lowerInput);
            if (num >= 1 && num <= 50) {
                log.info("【规则匹配】识别为方案选择：{}", num);
                return IntentType.SOLUTION_RECOMMENDATION;
            }
        } catch (NumberFormatException e) {
            // 不是纯数字，继续其他判断
        }

        // 2. 预订意图
        if (lowerInput.contains("预订") || lowerInput.contains("预定") ||
                lowerInput.contains("订") || lowerInput.contains("买票") ||
                lowerInput.contains("机票") || lowerInput.contains("酒店") ||
                lowerInput.contains("火车") || lowerInput.contains("高铁") ||
                lowerInput.contains("动车") || lowerInput.contains("用车") ||
                lowerInput.contains("出差") || lowerInput.contains("差旅")) {
            return IntentType.BOOKING;
        }

        // 3. 咨询意图
        if (lowerInput.contains("政策") || lowerInput.contains("规定") ||
                lowerInput.contains("标准") || lowerInput.contains("怎么报销") ||
                lowerInput.contains("流程") || lowerInput.contains("如何") ||
                lowerInput.contains("多少钱") || lowerInput.contains("预算") ||
                lowerInput.contains("为什么") || lowerInput.contains("吗") ||
                lowerInput.contains("?") || lowerInput.contains("？")) {
            return IntentType.CONSULTATION;
        }

        // 4. 闲聊意图
        if (lowerInput.contains("你好") || lowerInput.contains("您好") ||
                lowerInput.contains("谢谢") || lowerInput.contains("感谢") ||
                lowerInput.contains("再见") || lowerInput.contains("拜拜") ||
                lowerInput.contains("hello") || lowerInput.contains("hi ") ||
                lowerInput.contains("早上好") || lowerInput.contains("晚上好")) {
            return IntentType.CHITCHAT;
        }

        // 5. 默认为提供信息
        return IntentType.PROVIDING_INFO;
    }

}
