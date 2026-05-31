package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jullyscraft.entity.Shipment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrackingResponse {
    private String                   awbNumber;
    private String                   orderNumber;
    private Shipment.ShippingProvider provider;
    private Shipment.ShipmentStatus  status;
    private String                   courierName;
    private String                   trackingUrl;
    private LocalDateTime            estimatedDelivery;
    private LocalDateTime            shippedAt;
    private LocalDateTime            deliveredAt;
    private List<TrackingEvent>      events;

    @Getter
    @Builder
    public static class TrackingEvent {
        private LocalDateTime timestamp;
        private String        status;
        private String        location;
        private String        description;
    }
}