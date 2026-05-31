package com.jullyscraft.mapper;

import com.jullyscraft.dto.response.PaymentResponse;
import com.jullyscraft.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "orderId", source = "order.id")
    PaymentResponse toResponse(Payment payment);
}