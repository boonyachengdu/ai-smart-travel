package com.boonya.business.trip.dialog.handler;

import com.boonya.business.trip.common.entity.Passenger;
import com.boonya.business.trip.common.models.dialog.OrderRequirements;
import org.springframework.stereotype.Service;

@Service
public class OrderResponseHandler {

    public String generateOrderConfirmResponse(OrderRequirements order) {
        StringBuilder sb = new StringBuilder();
        sb.append("已为您生成订单，请确认信息：\n\n");
        sb.append("订单类型：").append(order.getOrderType()).append("\n");
        sb.append("行程信息：").append(order.getDepartureCity()).append(" -> ").append(order.getArrivalCity()).append("\n");
        sb.append("出发时间：").append(order.getDepartureDate()).append("\n");
        sb.append("订单预算：").append(order.getBudget()).append("元\n");

        if (order.getPassengers() != null && !order.getPassengers().isEmpty()) {
            sb.append("乘客：");
            for (Passenger p : order.getPassengers()) {
                sb.append(p.getName()).append(" ");
            }
            sb.append("\n");
        }

        sb.append("\n确认无误后，请回复“确认下单”");
        return sb.toString();
    }

    public String generateViolationResponse(String reason) {
        return String.format("订单不符合企业差旅标准：%s\n\n请您调整后重新下单，或联系管理员申请特批。", reason);
    }
}
