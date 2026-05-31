package com.jullyscraft.mapper;

import com.jullyscraft.dto.response.*;
import com.jullyscraft.entity.Order;
import com.jullyscraft.entity.OrderItem;
import com.jullyscraft.entity.OrderStatusHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "userId",      source = "user.id")
    @Mapping(target = "cancellable", expression = "java(order.isCancellable())")
    @Mapping(target = "returnable",  expression = "java(order.isReturnable())")
    OrderResponse toResponse(Order order);

    @Mapping(target = "itemCount",    expression = "java(order.getItems().size())")
    @Mapping(target = "shippingCity", source = "shippingCity")
    OrderSummaryResponse toSummary(Order order);

    @Mapping(target = "productId", source = "product.id")
    OrderItemResponse toItemResponse(OrderItem item);

    @Mapping(target = "changedAt", source = "createdAt")
    OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory history);
}