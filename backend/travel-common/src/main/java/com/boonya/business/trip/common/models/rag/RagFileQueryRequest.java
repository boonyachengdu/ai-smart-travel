package com.boonya.business.trip.common.models.rag;

import lombok.Data;

@Data
public class RagFileQueryRequest {
    private Integer page = 1;
    private Integer size = 10;
    private String fileName;
    private String mimeType;
    private Long companyId;
}
