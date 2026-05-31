package com.jullyscraft.dto.response;

import com.jullyscraft.entity.Order;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class OrderStatusHistoryResponse {
    private Order.OrderStatus fromStatus;
    private Order.OrderStatus toStatus;
    private String            note;
    private String            changedBy;
    private LocalDateTime     changedAt;
}