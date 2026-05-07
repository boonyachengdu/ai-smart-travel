package com.boonya.business.trip.rag.evaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationSample {

    private String query;
    private List<String> retrievedIds;
    private Set<String> relevantIds;
}
