package com.boonya.business.trip.rag.evaluation;

import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户反馈收集器 —— 通过用户采纳/拒绝行为隐式评估检索质量
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackCollector {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String FEEDBACK_KEY_PREFIX = "rag:feedback:";
    private static final String EVAL_SAMPLE_KEY = "rag:eval:samples";
    private static final int MAX_SAMPLES = 1000;

    /**
     * 记录用户反馈（正面：用户采用了检索结果中的信息）
     */
    public void recordPositive(String query, List<String> retrievedDocIds, String sessionId) {
        recordFeedback(query, retrievedDocIds, sessionId, true);
    }

    /**
     * 记录用户反馈（负面：用户忽略或纠正了检索结果）
     */
    public void recordNegative(String query, List<String> retrievedDocIds, String sessionId) {
        recordFeedback(query, retrievedDocIds, sessionId, false);
    }

    private void recordFeedback(String query, List<String> retrievedDocIds, String sessionId, boolean positive) {
        try {
            JSONObject feedback = new JSONObject();
            feedback.put("query", query);
            feedback.put("docIds", retrievedDocIds);
            feedback.put("positive", positive);
            feedback.put("sessionId", sessionId);
            feedback.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String key = FEEDBACK_KEY_PREFIX + sessionId + ":" + System.currentTimeMillis();
            stringRedisTemplate.opsForValue().set(key, feedback.toJSONString(), 7, TimeUnit.DAYS);

            // 累积为评估样本
            if (positive) {
                appendEvalSample(query, Set.copyOf(retrievedDocIds));
            }

            log.debug("反馈已记录: query={}, positive={}", query, positive);
        } catch (Exception e) {
            log.error("记录反馈失败", e);
        }
    }

    /**
     * 获取最近的反馈作为评估样本
     */
    public List<EvaluationSample> getEvaluationSamples(int limit) {
        try {
            String raw = stringRedisTemplate.opsForValue().get(EVAL_SAMPLE_KEY);
            if (raw == null) return List.of();

            List<JSONObject> samples = JSONObject.parseArray(raw, JSONObject.class);
            if (samples == null || samples.isEmpty()) return List.of();

            return samples.stream()
                    .limit(limit)
                    .map(obj -> EvaluationSample.builder()
                            .query(obj.getString("query"))
                            .relevantIds(Set.copyOf(obj.getJSONArray("relevantIds").toJavaList(String.class)))
                            .retrievedIds(List.of()) // 这里不追踪检索ID，仅追踪相关ID
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取评估样本失败", e);
            return List.of();
        }
    }

    private void appendEvalSample(String query, Set<String> relevantIds) {
        try {
            String raw = stringRedisTemplate.opsForValue().get(EVAL_SAMPLE_KEY);
            List<JSONObject> samples;
            if (raw != null && !raw.isEmpty()) {
                samples = new ArrayList<>(JSONObject.parseArray(raw, JSONObject.class));
            } else {
                samples = new ArrayList<>();
            }

            JSONObject sample = new JSONObject();
            sample.put("query", query);
            sample.put("relevantIds", relevantIds);
            samples.add(sample);

            // 保持上限
            while (samples.size() > MAX_SAMPLES) {
                samples.remove(0);
            }

            stringRedisTemplate.opsForValue().set(EVAL_SAMPLE_KEY, JSONObject.toJSONString(samples));
        } catch (Exception e) {
            log.error("追加评估样本失败", e);
        }
    }
}
