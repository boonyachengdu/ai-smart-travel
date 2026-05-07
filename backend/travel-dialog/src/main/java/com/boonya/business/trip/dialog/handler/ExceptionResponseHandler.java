package com.boonya.business.trip.dialog.handler;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExceptionResponseHandler {

    public String handleApiError() {
        return "AI服务暂时不可用，请稍后重试。\n\n您也可以前往订单管理页面直接下单。";
    }

    public String handleTimeoutError() {
        return "请求处理超时，请稍后重试。\n\n如果问题持续出现，请联系技术支持。";
    }

    public String handleInvalidInput(String userInput) {
        return "抱歉，我没有理解您的意思。\n\n您可以尝试：\n• “帮我订明天从北京到上海的机票”\n• “查询我的订单”\n• “差旅标准是什么”";
    }

    public String handleAmbiguousIntent(List<String> possibleIntents) {
        StringBuilder sb = new StringBuilder();
        sb.append("您是想：\n");
        for (int i = 0; i < possibleIntents.size(); i++) {
            sb.append(i + 1).append(". ").append(possibleIntents.get(i)).append("\n");
        }
        sb.append("\n请输入序号选择，或详细描述您的需求。");
        return sb.toString();
    }
}
