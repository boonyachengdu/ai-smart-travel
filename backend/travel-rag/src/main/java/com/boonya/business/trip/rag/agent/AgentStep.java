package com.boonya.business.trip.rag.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStep {

    private int iteration;
    private String thought;
    private String action;
    private String actionInput;
    private String observation;
    private LocalDateTime timestamp;
}
