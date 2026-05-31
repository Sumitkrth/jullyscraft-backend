package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jullyscraft.entity.Shipment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShippingRateResponse {
    private String                   pickupPincode;
    private String                   deliveryPincode;
    private List<CourierOption>      options;
    private BigDecimal               recommendedRate;
    private String                   recommendedCourier;
    private boolean                  codAvailable;

    @Getter
    @Builder
    public static class CourierOption {
        private Shipment.ShippingProvider provider;
        private String                    courierName;
        private String                    courierCode;
        private BigDecimal                rate;
        private BigDecimal                codCharge;
        private int                       estimatedDays;
        private boolean                   codAvailable;
        private boolean                   recommended;
    }
}