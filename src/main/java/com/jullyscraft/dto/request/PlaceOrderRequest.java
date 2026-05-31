package com.jullyscraft.dto.request;

import com.jullyscraft.entity.Order;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PlaceOrderRequest {

    // Shipping — either pick saved address OR provide inline
    private Long addressId;                  // use saved address

    // Inline address (for guests or override)
    private String shippingFullName;
    private String shippingPhone;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;
    private String shippingCountry;

    @NotNull(message = "Payment method is required")
    private Order.PaymentMethod paymentMethod;

    private String couponCode;

    @Size(max = 500)
    private String customerNote;

    // Guest checkout
    @Email
    private String guestEmail;
}