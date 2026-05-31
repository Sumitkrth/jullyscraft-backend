package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReferralResponse {
    private String     code;
    private String     referralUrl;
    private BigDecimal referrerReward;
    private BigDecimal refereeDiscount;
    private int        timesUsed;
    private Integer    maxUses;
    private boolean    active;
    private LocalDateTime expiresAt;
}