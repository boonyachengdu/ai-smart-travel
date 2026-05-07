package com.boonya.business.trip.rag.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private String content;
    private double vectorScore;
    private double keywordScore;
    private double fusedScore;
    private Map<String, Object> metadata;

    public String getFormattedContent() {
        return content != null ? content : "";
    }
}
