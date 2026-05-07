package com.boonya.business.trip.rag.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent工作记忆 —— 跨会话保留关键信息摘要
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentWorkingMemory {

    private String userId;
    private Long companyId;

    /** 用户偏好的差旅类型（从历史推断） */
    @Builder.Default
    private List<String> preferredScenes = new ArrayList<>();

    /** 最近搜索的主题摘要 */
    @Builder.Default
    private List<String> recentTopics = new ArrayList<>();

    /** 已确认的公司政策要点 */
    @Builder.Default
    private List<String> confirmedPolicies = new ArrayList<>();

    /** 上次合规检查结果 */
    private String lastComplianceResult;

    /** 上次活跃时间 */
    private LocalDateTime lastActiveTime;

    /** 会话计数 */
    @Builder.Default
    private int sessionCount = 0;

    /** 用户满意度均值（0-1） */
    @Builder.Default
    private double avgSatisfaction = 0.0;

    public String toContextString() {
        if (recentTopics.isEmpty() && confirmedPolicies.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【用户历史上下文】\n");

        if (!preferredScenes.isEmpty()) {
            sb.append("偏好的差旅场景：").append(String.join("、", preferredScenes)).append("\n");
        }
        if (!recentTopics.isEmpty()) {
            sb.append("最近关注话题：").append(String.join("、", recentTopics)).append("\n");
        }
        if (!confirmedPolicies.isEmpty()) {
            sb.append("已确认政策：\n");
            for (int i = 0; i < confirmedPolicies.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(confirmedPolicies.get(i)).append("\n");
            }
        }
        if (lastComplianceResult != null) {
            sb.append("上次合规结果：").append(lastComplianceResult).append("\n");
        }

        return sb.toString();
    }
}
