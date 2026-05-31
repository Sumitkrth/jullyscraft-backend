package com.jullyscraft.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ShippingRateRequest {

    @NotBlank(message = "Pickup pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid pickup pincode")
    private String pickupPincode;

    @NotBlank(message = "Delivery pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid delivery pincode")
    private String deliveryPincode;

    @NotNull(message = "Weight is required")
    @Min(value = 1, message = "Weight must be at least 1 gram")
    private Integer weightGrams;

    @DecimalMin(value = "0.01")
    private BigDecimal declaredValue;

    private int length  = 15;
    private int breadth = 12;
    private int height  = 10;
}