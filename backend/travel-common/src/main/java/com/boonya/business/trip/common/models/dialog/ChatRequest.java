package com.boonya.business.trip.common.models.dialog;

import com.boonya.business.trip.common.constant.Scene;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    /**
     * 是否是新会话
     */
    private Boolean newSession;
    /**
     * 场景：HOTEL、FLIGHT、TRAIN、CAR、QA
     */
    private Scene scene; // 聊天场景
    /**
     * 用户输入
     */
    private String userInput;//用户输入

    /**
     * 消息列表（用于多轮对话）
     */
    private List<Map<String, String>> messages = new ArrayList<>();

    /**
     * 添加消息到列表
     * @param role 角色：system、user、assistant
     * @param content 消息内容
     */
    public void addMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        this.messages.add(message);
    }
}
