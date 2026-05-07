package com.boonya.business.trip.rag.evaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationReport {

    private double mrr;
    private double precisionAt3;
    private double precisionAt5;
    private double recallAt3;
    private int sampleCount;
}
