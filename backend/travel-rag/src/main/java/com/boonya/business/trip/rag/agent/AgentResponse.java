package com.boonya.business.trip.rag.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    private String answer;
    private List<AgentStep> steps;
    private int totalIterations;
    private String contextUsed;
    private boolean forcedStop;
}
