package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ConversionReportResponse {

    // ── Funnel ────────────────────────────────────────────────────────────────
    private long   productViews;
    private long   addToCartEvents;
    private long   checkoutStarts;
    private long   ordersPlaced;

    // ── Conversion rates ──────────────────────────────────────────────────────
    private double viewToCartRate;        // addToCart / productViews
    private double cartToCheckoutRate;    // checkoutStart / addToCart
    private double checkoutToOrderRate;   // ordersPlaced / checkoutStart
    private double overallConversionRate; // ordersPlaced / productViews

    // ── Session metrics ───────────────────────────────────────────────────────
    private long   totalSessions;
    private long   bounceSessions;
    private double bounceRate;

    // ── Device breakdown ──────────────────────────────────────────────────────
    private Map<String, Long> deviceBreakdown;

    // ── Cart abandonment ──────────────────────────────────────────────────────
    private long   cartAbandonments;
    private double cartAbandonmentRate;
}