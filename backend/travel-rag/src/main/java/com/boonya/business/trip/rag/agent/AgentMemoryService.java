package com.boonya.business.trip.rag.agent;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Agent工作记忆服务 —— 基于Redis的跨会话记忆管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentMemoryService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String MEMORY_KEY_PREFIX = "agent:memory:";
    private static final int MAX_TOPICS = 10;
    private static final int MAX_POLICIES = 20;
    private static final int MEMORY_TTL_DAYS = 30;

    /**
     * 加载用户的工作记忆
     */
    public AgentWorkingMemory load(String userId) {
        try {
            String key = MEMORY_KEY_PREFIX + userId;
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                return AgentWorkingMemory.builder()
                        .userId(userId)
                        .lastActiveTime(LocalDateTime.now())
                        .build();
            }
            return JSONObject.parseObject(json, AgentWorkingMemory.class);
        } catch (Exception e) {
            log.error("加载Agent记忆失败, userId={}", userId, e);
            return AgentWorkingMemory.builder()
                    .userId(userId)
                    .lastActiveTime(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 保存用户的工作记忆
     */
    public void save(AgentWorkingMemory memory) {
        try {
            String key = MEMORY_KEY_PREFIX + memory.getUserId();
            String json = JSONObject.toJSONString(memory);
            stringRedisTemplate.opsForValue().set(key, json, MEMORY_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("保存Agent记忆失败, userId={}", memory.getUserId(), e);
        }
    }

    /**
     * 记录一次会话的主题
     */
    public void recordTopic(String userId, String topic) {
        AgentWorkingMemory memory = load(userId);
        List<String> topics = memory.getRecentTopics();
        // 去重，最新的在前
        topics.remove(topic);
        topics.add(0, topic);
        while (topics.size() > MAX_TOPICS) {
            topics.remove(topics.size() - 1);
        }
        memory.setLastActiveTime(LocalDateTime.now());
        memory.setSessionCount(memory.getSessionCount() + 1);
        save(memory);
    }

    /**
     * 记录已确认的政策
     */
    public void confirmPolicy(String userId, String policyPoint) {
        AgentWorkingMemory memory = load(userId);
        List<String> policies = memory.getConfirmedPolicies();
        if (!policies.contains(policyPoint)) {
            policies.add(policyPoint);
            while (policies.size() > MAX_POLICIES) {
                policies.remove(0);
            }
        }
        memory.setLastActiveTime(LocalDateTime.now());
        save(memory);
    }

    /**
     * 记录合规检查结果
     */
    public void recordComplianceResult(String userId, String result) {
        AgentWorkingMemory memory = load(userId);
        memory.setLastComplianceResult(result);
        memory.setLastActiveTime(LocalDateTime.now());
        save(memory);
    }

    /**
     * 记录用户满意度
     */
    public void recordSatisfaction(String userId, double score) {
        AgentWorkingMemory memory = load(userId);
        int n = memory.getSessionCount();
        double old = memory.getAvgSatisfaction();
        memory.setAvgSatisfaction((old * n + score) / (n + 1));
        save(memory);
    }

    /**
     * 获取记忆上下文（用于注入System Prompt）
     */
    public String getMemoryContext(String userId) {
        AgentWorkingMemory memory = load(userId);
        return memory.toContextString();
    }

    /**
     * 清除过期记忆
     */
    public void clear(String userId) {
        stringRedisTemplate.delete(MEMORY_KEY_PREFIX + userId);
        log.info("已清除Agent记忆: userId={}", userId);
    }
}
