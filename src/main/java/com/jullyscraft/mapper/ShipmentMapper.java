package com.jullyscraft.mapper;

import com.jullyscraft.dto.response.ShipmentResponse;
import com.jullyscraft.entity.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {

    @Mapping(target = "orderId",     source = "order.id")
    @Mapping(target = "orderNumber", source = "order.orderNumber")
    ShipmentResponse toResponse(Shipment shipment);
}