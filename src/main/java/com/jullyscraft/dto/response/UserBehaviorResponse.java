package com.jullyscraft.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class UserBehaviorResponse {

    private long               totalPageViews;
    private long               uniqueSessions;
    private long               activeUsersNow;        // last 5 minutes
    private long               activeUsersToday;
    private double             avgSessionDuration;    // minutes (estimated)

    // ── Top content ───────────────────────────────────────────────────────────
    private List<String>       topSearchQueries;
    private List<Long>         topViewedProductIds;

    // ── Device & traffic ──────────────────────────────────────────────────────
    private Map<String, Long>  deviceBreakdown;
    private Map<String, Long>  hourlyTraffic;         // hour → session count

    // ── User journey ──────────────────────────────────────────────────────────
    private long               newUsersCount;
    private long               returningUsersCount;
    private double             newUserRate;
}