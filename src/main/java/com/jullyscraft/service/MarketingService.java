package com.jullyscraft.service;

import com.jullyscraft.dto.request.AffiliateRequest;
import com.jullyscraft.dto.request.NewsletterSubscribeRequest;
import com.jullyscraft.dto.response.AffiliateResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ReferralResponse;
import com.jullyscraft.entity.AffiliatePartner;

public interface MarketingService {

    // ── Newsletter ────────────────────────────────────────────────────────────
    void   subscribe(NewsletterSubscribeRequest request);
    void   unsubscribeByEmail(String email);
    void   unsubscribeByToken(String token);
    long   getSubscriberCount();

    // ── Referral ──────────────────────────────────────────────────────────────
    ReferralResponse getOrCreateReferralCode(Long userId);
    ReferralResponse applyReferralCode(Long newUserId, String code);

    // ── Affiliate ─────────────────────────────────────────────────────────────
    AffiliateResponse            applyAffiliate(Long userId, AffiliateRequest request);
    AffiliateResponse            getAffiliateByCode(String code);
    AffiliateResponse            getMyAffiliate(Long userId);
    void                         recordAffiliateClick(String code);
    void                         recordAffiliateConversion(String code,
                                                           java.math.BigDecimal orderAmount);
    PageResponse<AffiliateResponse> getAllAffiliates(
            AffiliatePartner.AffiliateStatus status, int page, int size);
    AffiliateResponse            approveAffiliate(Long id);
    AffiliateResponse            suspendAffiliate(Long id);
}