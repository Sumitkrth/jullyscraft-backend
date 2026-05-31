package com.jullyscraft.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@Getter
public class ShippingConfig {

    @Value("${app.shipping.default-provider:SHIPROCKET}")
    private String defaultProvider;

    @Value("${app.shipping.free-shipping-threshold:499}")
    private BigDecimal freeShippingThreshold;

    @Value("${app.shipping.default-shipping-fee:49}")
    private BigDecimal defaultShippingFee;

    // Shiprocket
    @Value("${app.shipping.shiprocket.email:}")
    private String shiprocketEmail;

    @Value("${app.shipping.shiprocket.password:}")
    private String shiprocketPassword;

    @Value("${app.shipping.shiprocket.base-url:https://apiv2.shiprocket.in/v1/external}")
    private String shiprocketBaseUrl;

    @Value("${app.shipping.shiprocket.channel-id:}")
    private String shiprocketChannelId;

    // Delhivery
    @Value("${app.shipping.delhivery.token:}")
    private String delhiveryToken;

    @Value("${app.shipping.delhivery.base-url:https://track.delhivery.com}")
    private String delhiveryBaseUrl;

    @Value("${app.shipping.delhivery.warehouse-name:JullysCraft}")
    private String delhiveryWarehouseName;

    // Parcel defaults
    @Value("${app.shipping.weight-grams-default:500}")
    private int defaultWeightGrams;

    @Value("${app.shipping.dimensions.length:15}")
    private int defaultLength;

    @Value("${app.shipping.dimensions.breadth:12}")
    private int defaultBreadth;

    @Value("${app.shipping.dimensions.height:10}")
    private int defaultHeight;
}