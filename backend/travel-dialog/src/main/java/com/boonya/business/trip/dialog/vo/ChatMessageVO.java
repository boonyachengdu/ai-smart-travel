package com.boonya.business.trip.dialog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {
    private String role; // SYSTEM, USER, ASSISTANT
    private String content;
}
