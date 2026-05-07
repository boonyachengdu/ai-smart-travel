package com.boonya.business.trip.common.models.rag;

import lombok.Data;

/**
 * RAG 政策校验结果
 */
@Data
public class RagPolicyResult {

    /**
     * 政策文本（非结构化）
     */
    private String policyText;

    /**
     * 机票最大预算（结构化，可选）
     */
    private Double maxFlightBudget;

    /**
     * 酒店最大预算（结构化，可选）
     */
    private Double maxHotelBudget;

    /**
     * 是否有政策
     */
    public boolean hasPolicy;
}

