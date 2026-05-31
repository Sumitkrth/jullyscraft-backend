package com.jullyscraft.service.impl;

import com.jullyscraft.dto.request.AffiliateRequest;
import com.jullyscraft.dto.request.NewsletterSubscribeRequest;
import com.jullyscraft.dto.response.AffiliateResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.dto.response.ReferralResponse;
import com.jullyscraft.entity.*;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.DuplicateResourceException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.repository.*;
import com.jullyscraft.service.MarketingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketingServiceImpl implements MarketingService {

    private final NewsletterSubscriberRepository subscriberRepository;
    private final ReferralCodeRepository         referralRepository;
    private final AffiliatePartnerRepository     affiliateRepository;
    private final UserRepository                 userRepository;

    @Value("${app.notification.email.base-url:http://localhost:3000}")
    private String baseUrl;

    // ── Newsletter ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void subscribe(NewsletterSubscribeRequest req) {
        String email = req.getEmail().toLowerCase().trim();

        subscriberRepository.findByEmailAndDeletedFalse(email)
                .ifPresent(existing -> {
                    if (existing.isActive()) {
                        throw new DuplicateResourceException(
                                "Email is already subscribed");
                    }
                    // Re-subscribe
                    existing.setActive(true);
                    existing.setSubscribedAt(LocalDateTime.now());
                    existing.setUnsubscribedAt(null);
                    subscriberRepository.save(existing);
                    log.info("Re-subscribed: {}", email);
                });

        if (subscriberRepository.existsByEmailAndDeletedFalse(email)) return;

        NewsletterSubscriber subscriber = NewsletterSubscriber.builder()
                .email(email)
                .name(req.getName())
                .source(req.getSource())
                .active(true)
                .unsubscribeToken(UUID.randomUUID().toString())
                .subscribedAt(LocalDateTime.now())
                .build();

        subscriberRepository.save(subscriber);
        log.info("Newsletter subscribed: {}", email);
    }

    @Override
    @Transactional
    public void unsubscribeByEmail(String email) {
        subscriberRepository.unsubscribe(email.toLowerCase().trim());
        log.info("Newsletter unsubscribed: {}", email);
    }

    @Override
    @Transactional
    public void unsubscribeByToken(String token) {
        NewsletterSubscriber sub = subscriberRepository
                .findByUnsubscribeToken(token)
                .orElseThrow(() -> new BadRequestException(
                        "Invalid unsubscribe token"));
        sub.setActive(false);
        sub.setUnsubscribedAt(LocalDateTime.now());
        subscriberRepository.save(sub);
        log.info("Newsletter unsubscribed via token: {}", sub.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public long getSubscriberCount() {
        return subscriberRepository.countByActiveTrueAndDeletedFalse();
    }

    // ── Referral ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReferralResponse getOrCreateReferralCode(Long userId) {
        return referralRepository.findByOwnerIdAndDeletedFalse(userId)
                .map(this::toReferralResponse)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("User", "id", userId));

                    String code = generateReferralCode(user);

                    ReferralCode referral = ReferralCode.builder()
                            .code(code)
                            .owner(user)
                            .referrerReward(BigDecimal.valueOf(100))
                            .refereeDiscount(BigDecimal.valueOf(50))
                            .active(true)
                            .build();

                    return toReferralResponse(referralRepository.save(referral));
                });
    }

    @Override
    @Transactional
    public ReferralResponse applyReferralCode(Long newUserId, String code) {
        ReferralCode referral = referralRepository
                .findByCodeAndDeletedFalse(code.toUpperCase())
                .orElseThrow(() -> new BadRequestException(
                        "Invalid referral code: " + code));

        if (!referral.isValid()) {
            throw new BadRequestException("Referral code is expired or exhausted");
        }

        if (referral.getOwner().getId().equals(newUserId)) {
            throw new BadRequestException(
                    "You cannot use your own referral code");
        }

        referral.setTimesUsed(referral.getTimesUsed() + 1);
        referralRepository.save(referral);

        // TODO: credit referrer wallet, apply discount to new user order
        log.info("Referral code {} applied by user {}", code, newUserId);

        return toReferralResponse(referral);
    }

    // ── Affiliate ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AffiliateResponse applyAffiliate(Long userId, AffiliateRequest req) {
        if (affiliateRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException(
                    "You have already applied for an affiliate account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String code = generateAffiliateCode(req.getCompanyName());

        AffiliatePartner partner = AffiliatePartner.builder()
                .user(user)
                .affiliateCode(code)
                .companyName(req.getCompanyName())
                .websiteUrl(req.getWebsiteUrl())
                .commissionPercent(BigDecimal.valueOf(5.0))
                .status(AffiliatePartner.AffiliateStatus.PENDING)
                .build();

        return toAffiliateResponse(affiliateRepository.save(partner));
    }

    @Override
    @Transactional(readOnly = true)
    public AffiliateResponse getAffiliateByCode(String code) {
        AffiliatePartner partner = affiliateRepository
                .findByAffiliateCode(code.toUpperCase())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Affiliate", "code", code));
        return toAffiliateResponse(partner);
    }

    @Override
    @Transactional(readOnly = true)
    public AffiliateResponse getMyAffiliate(Long userId) {
        AffiliatePartner partner = affiliateRepository
                .findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Affiliate account not found for user", "userId", userId));
        return toAffiliateResponse(partner);
    }

    @Override
    @Transactional
    public void recordAffiliateClick(String code) {
        affiliateRepository.findByAffiliateCode(code.toUpperCase())
                .ifPresent(p -> {
                    p.setTotalClicks(p.getTotalClicks() + 1);
                    affiliateRepository.save(p);
                });
    }

    @Override
    @Transactional
    public void recordAffiliateConversion(String code, BigDecimal orderAmount) {
        affiliateRepository.findByAffiliateCode(code.toUpperCase())
                .ifPresent(p -> {
                    BigDecimal commission = orderAmount
                            .multiply(p.getCommissionPercent())
                            .divide(BigDecimal.valueOf(100),
                                    2, java.math.RoundingMode.HALF_UP);

                    p.setTotalConversions(p.getTotalConversions() + 1);
                    p.setTotalEarnings(p.getTotalEarnings().add(commission));
                    p.setPendingPayout(p.getPendingPayout().add(commission));
                    affiliateRepository.save(p);

                    log.info("Affiliate {} earned ₹{} commission on ₹{} order",
                            code, commission, orderAmount);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AffiliateResponse> getAllAffiliates(
            AffiliatePartner.AffiliateStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        var result = status != null
                ? affiliateRepository.findByStatus(status, pageable)
                : affiliateRepository.findAll(pageable);

        return PageResponse.of(result.map(this::toAffiliateResponse));
    }

    @Override
    @Transactional
    public AffiliateResponse approveAffiliate(Long id) {
        AffiliatePartner partner = affiliateRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Affiliate", "id", id));
        partner.setStatus(AffiliatePartner.AffiliateStatus.APPROVED);
        return toAffiliateResponse(affiliateRepository.save(partner));
    }

    @Override
    @Transactional
    public AffiliateResponse suspendAffiliate(Long id) {
        AffiliatePartner partner = affiliateRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Affiliate", "id", id));
        partner.setStatus(AffiliatePartner.AffiliateStatus.SUSPENDED);
        return toAffiliateResponse(affiliateRepository.save(partner));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String generateReferralCode(User user) {
        String base = user.getFirstName().toUpperCase()
                .replaceAll("[^A-Z]", "")
                .substring(0, Math.min(4, user.getFirstName().length()));
        String suffix = String.format("%04d", new Random().nextInt(9999));
        String code   = base + suffix;

        // Ensure uniqueness
        while (referralRepository.existsByCodeAndDeletedFalse(code)) {
            suffix = String.format("%04d", new Random().nextInt(9999));
            code   = base + suffix;
        }
        return code;
    }

    private String generateAffiliateCode(String companyName) {
        String base = companyName.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, Math.min(6, companyName.length()));
        String suffix = String.valueOf(System.currentTimeMillis()).substring(8);
        return (base + suffix).toUpperCase();
    }

    private ReferralResponse toReferralResponse(ReferralCode r) {
        return ReferralResponse.builder()
                .code(r.getCode())
                .referralUrl(baseUrl + "/register?ref=" + r.getCode())
                .referrerReward(r.getReferrerReward())
                .refereeDiscount(r.getRefereeDiscount())
                .timesUsed(r.getTimesUsed())
                .maxUses(r.getMaxUses())
                .active(r.isActive())
                .expiresAt(r.getExpiresAt())
                .build();
    }

    private AffiliateResponse toAffiliateResponse(AffiliatePartner p) {
        AffiliateResponse res = new AffiliateResponse();
        res.setId(p.getId());
        res.setAffiliateCode(p.getAffiliateCode());
        res.setCompanyName(p.getCompanyName());
        res.setWebsiteUrl(p.getWebsiteUrl());
        res.setCommissionPercent(p.getCommissionPercent());
        res.setTotalEarnings(p.getTotalEarnings());
        res.setPendingPayout(p.getPendingPayout());
        res.setTotalClicks(p.getTotalClicks());
        res.setTotalConversions(p.getTotalConversions());
        res.setStatus(p.getStatus());
        res.setAffiliateUrl(baseUrl + "?aff=" + p.getAffiliateCode());
        return res;
    }
}