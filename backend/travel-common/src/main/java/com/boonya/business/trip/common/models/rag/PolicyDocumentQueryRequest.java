package com.boonya.business.trip.common.models.rag;

import com.boonya.business.trip.common.constant.Scene;
import lombok.Data;
import lombok.NonNull;

@Data
public class PolicyDocumentQueryRequest {
    private Scene scene;
    private Long companyId;
    private Long deptId;
    private String query;
    private Integer limit = 5;
    private Integer page = 1;
    private Integer size = 10;
}
