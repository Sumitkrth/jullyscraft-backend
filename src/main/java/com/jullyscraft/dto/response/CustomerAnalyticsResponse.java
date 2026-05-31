package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class CustomerAnalyticsResponse {
    private long                totalCustomers;
    private long                newCustomersThisMonth;
    private long                returningCustomers;
    private double              retentionRate;
    private BigDecimal          averageLifetimeValue;
    private Map<String, Long>   customersByCity;
    private List<String>        topCities;
    private Map<Integer, Long>  orderFrequencyDistribution; // orders placed → count of users
}