package com.boonya.business.trip.order.event;

import org.springframework.context.ApplicationEvent;

/**
 * 创建渠道订单事件：订单审核通过才会发往渠道下单
 */
public class CreateChannelOrderEvent extends ApplicationEvent {
    public CreateChannelOrderEvent(String orderNo) {
        super(new CreateChannelOrderEventSource(orderNo));
    }

    public static class CreateChannelOrderEventSource {
        private String orderNo;

        public CreateChannelOrderEventSource(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }
    }

}
