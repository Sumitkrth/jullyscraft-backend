package com.jullyscraft.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jullyscraft.entity.AffiliatePartner;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AffiliateResponse {
    private Long                         id;
    private String                       affiliateCode;
    private String                       companyName;
    private String                       websiteUrl;
    private BigDecimal                   commissionPercent;
    private BigDecimal                   totalEarnings;
    private BigDecimal                   pendingPayout;
    private long                         totalClicks;
    private long                         totalConversions;
    private AffiliatePartner.AffiliateStatus status;
    private String                       affiliateUrl;
}