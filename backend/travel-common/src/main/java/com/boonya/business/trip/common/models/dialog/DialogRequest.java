package com.boonya.business.trip.common.models.dialog;

import com.boonya.business.trip.common.constant.Scene;
import lombok.Data;

@Data
public class DialogRequest {
    private Scene scene;
    private String question;
}
