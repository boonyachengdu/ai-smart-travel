package com.boonya.business.trip.common.models.dialog;

import com.boonya.business.trip.common.models.dialog.constant.IntentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 意图模型类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Intent {

    /**
     * 意图类型
     */
    private IntentType intentType;

    /**
     * 意图名称（中文）
     */
    private String intentName;

    /**
     * 置信度（0.0-1.0）
     */
    private Double confidence;

    /**
     * 提取的实体信息
     */
    private Map<String, Object> entities;

    /**
     * 原始文本
     */
    private String originalText;

    /**
     * 优先级（用于多意图排序，数值越小优先级越高）
     */
    private Integer priority;

    /**
     * 判断理由（AI 生成）
     */
    private String reason;

    /**
     * 获取意图类型的代码
     */
    public String getIntentCode() {
        return intentType != null ? intentType.getCode() : null;
    }

    /**
     * 获取意图类型的中文名称
     */
    public String getIntentChineseName() {
        return intentType != null ? intentType.getName() : null;
    }
}
