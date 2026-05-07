package com.boonya.business.trip.common.models.rag;

import lombok.Data;
@Data
public class PromptQueryRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String name;
    private Boolean enabled;
}
