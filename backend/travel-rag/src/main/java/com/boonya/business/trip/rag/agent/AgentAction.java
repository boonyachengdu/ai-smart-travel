package com.boonya.business.trip.rag.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAction {

    private String thought;
    private String action;
    private String actionInput;
    private Integer confidence;
}
