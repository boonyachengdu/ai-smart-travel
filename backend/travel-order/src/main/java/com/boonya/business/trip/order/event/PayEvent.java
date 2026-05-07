package com.boonya.business.trip.order.event;

import org.springframework.context.ApplicationEvent;

public class PayEvent extends ApplicationEvent {

    public PayEvent(Object source) {
        super(source);
    }
}
