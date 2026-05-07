package com.boonya.business.trip.dialog.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotExtractionService {
    private final ChatClient chatClient;

    public Map<String, Object> extractSlots(String prompt, String userInput, Map<String, Object> currentSlots, String conversationId) {
        try {
            String contextInfo = "当前已有信息：" + JSON.toJSONString(currentSlots) + "\n用户输入：" + userInput;

            String result = chatClient.prompt()
                    .system(prompt)
                    .user(contextInfo)
                    .advisors(advisor -> advisor.param("chatMemoryContextId", "slot_" + conversationId))
                    .call()
                    .content();

            log.debug("AI 响应：{}", result);

            return parseSlotResponse(result, currentSlots);
        } catch (Exception e) {
            log.error("槽位提取失败", e);
            return currentSlots != null ? currentSlots : new HashMap<>();
        }
    }

    public String generateQuestion(Map<String, Object> missingSlots) {
        if (missingSlots == null || missingSlots.isEmpty()) {
            return "请问还有什么可以帮您？";
        }

        if (missingSlots.containsKey("orderType") || missingSlots.containsKey("order_type")) {
            return "请问您想预订什么？（机票/酒店/火车/用车）";
        }
        if (missingSlots.containsKey("departureCity") || missingSlots.containsKey("departure")) {
            return "请问您的出发城市是哪里？";
        }
        if (missingSlots.containsKey("arrivalCity") || missingSlots.containsKey("arrival")) {
            return "请问您的目的地是哪里？";
        }
        if (missingSlots.containsKey("departureDate") || missingSlots.containsKey("departure_date")) {
            return "请问您计划何时出发？（格式：YYYY-MM-DD）";
        }
        if (missingSlots.containsKey("returnDate") || missingSlots.containsKey("return_date")) {
            return "请问您是否需要返程？如果是，返程日期是？";
        }
        if (missingSlots.containsKey("passengers")) {
            return "请问出行人是谁？（姓名）";
        }
        if (missingSlots.containsKey("budget")) {
            return "请问您的预算是多少？";
        }
        if (missingSlots.containsKey("checkInCity")) {
            return "请问入住哪个城市？";
        }
        if (missingSlots.containsKey("checkInDate")) {
            return "请问入住日期是？";
        }
        if (missingSlots.containsKey("checkOutDate")) {
            return "请问离店日期是？";
        }
        if (missingSlots.containsKey("city")) {
            return "请问在哪个城市用车？";
        }
        if (missingSlots.containsKey("useTime")) {
            return "请问用车时间是？";
        }
        if (missingSlots.containsKey("destination")) {
            return "请问目的地是？";
        }

        return "请告诉我更多关于您的出行计划的信息。";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSlotResponse(String response, Map<String, Object> currentSlots) {
        if (response == null || response.trim().isEmpty()) {
            log.warn("AI 响应为空");
            return currentSlots != null ? currentSlots : new HashMap<>();
        }

        try {
            String jsonContent = extractJsonFromResponse(response);

            log.debug("提取的 JSON 内容：{}", jsonContent);

            JSONObject jsonResponse = JSON.parseObject(jsonContent);

            Map<String, Object> result = currentSlots != null ? currentSlots : new HashMap<>();

            String status = jsonResponse.getString("status");

            if ("complete".equals(status)) {
                JSONObject order = jsonResponse.getJSONObject("order");
                if (order != null) {
                    if (order.containsKey("orderType")) {
                        result.put("orderType", order.getString("orderType"));
                    }

                    if (order.containsKey("departureCity")) {
                        result.put("departureCity", order.getString("departureCity"));
                    }
                    if (order.containsKey("arrivalCity")) {
                        result.put("arrivalCity", order.getString("arrivalCity"));
                    }

                    if (order.containsKey("departureDate")) {
                        result.put("departureDate", order.getString("departureDate"));
                    }
                    if (order.containsKey("returnDate")) {
                        result.put("returnDate", order.getString("returnDate"));
                    }

                    if (order.containsKey("passengers")) {
                        result.put("passengers", order.getJSONArray("passengers"));
                    }

                    if (order.containsKey("budget")) {
                        result.put("budget", order.getObject("budget", Object.class));
                    }

                    if (order.containsKey("specialRequirements")) {
                        result.put("specialRequirements", order.getString("specialRequirements"));
                    }
                }

                log.info("槽位提取完成，状态：complete, 结果：{}", result);

            } else if ("clarify".equals(status)) {
                List<String> questions = jsonResponse.getJSONArray("clarifyQuestions").toJavaList(String.class);
                if (questions != null && !questions.isEmpty()) {
                    result.put("pendingQuestions", questions);
                    log.info("需要澄清的问题：{}", questions);
                }

            } else if ("violation".equals(status)) {
                String violationReason = jsonResponse.getString("violationReason");
                result.put("violationReason", violationReason);
                log.warn("检测到违反差标规则：{}", violationReason);
            }

            return result;

        } catch (Exception e) {
            log.error("解析槽位响应失败：{}", response, e);
            return currentSlots != null ? currentSlots : new HashMap<>();
        }
    }

    private String extractJsonFromResponse(String response) {
        if (response == null) {
            return "{}";
        }

        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        log.warn("未能在响应中找到 JSON 结构：{}", response);
        return "{}";
    }
}
