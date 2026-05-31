package com.jullyscraft.service.impl;

import com.jullyscraft.dto.request.ApplyCouponRequest;
import com.jullyscraft.dto.request.CreateCouponRequest;
import com.jullyscraft.dto.response.ApplyCouponResponse;
import com.jullyscraft.dto.response.CouponResponse;
import com.jullyscraft.dto.response.PageResponse;
import com.jullyscraft.entity.*;
import com.jullyscraft.exception.BadRequestException;
import com.jullyscraft.exception.DuplicateResourceException;
import com.jullyscraft.exception.ResourceNotFoundException;
import com.jullyscraft.mapper.CouponMapper;
import com.jullyscraft.repository.*;
import com.jullyscraft.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository      couponRepository;
    private final CouponUsageRepository usageRepository;
    private final UserRepository        userRepository;
    private final OrderRepository       orderRepository;
    private final CouponMapper          couponMapper;

    // ── Public: validate & apply ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApplyCouponResponse validateAndApply(Long userId, ApplyCouponRequest req) {
        Coupon coupon = couponRepository
                .findByCodeAndDeletedFalse(req.getCode().toUpperCase())
                .orElseThrow(() -> new BadRequestException(
                        "Coupon code is invalid: " + req.getCode()));

        validateCoupon(coupon, userId, req);

        return calculateDiscount(coupon, req.getOrderAmount());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getActiveFlashSales() {
        return couponRepository.findActiveFlashSales(LocalDateTime.now())
                .stream()
                .map(couponMapper::toResponse)
                .toList();
    }

    // ── Order lifecycle hooks ─────────────────────────────────────────────────

    @Override
    @Transactional
    public void recordUsage(Coupon coupon, Order order,
                            Long userId, BigDecimal discountApplied) {
        User user = userId != null
                ? userRepository.findById(userId).orElse(null)
                : null;

        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .order(order)
                .user(user)
                .discountApplied(discountApplied)
                .build();

        usageRepository.save(usage);
        couponRepository.incrementUsage(coupon.getId());
        log.info("Coupon {} used on order {}", coupon.getCode(), order.getOrderNumber());
    }

    @Override
    @Transactional
    public void releaseUsage(Long orderId) {
        usageRepository.findByCouponIdAndOrderId(
                        // find by order — load usage to get coupon
                        -1L, orderId)
                .ifPresent(usage -> {
                    couponRepository.decrementUsage(usage.getCoupon().getId());
                    usageRepository.delete(usage);
                    log.info("Coupon usage released for order {}", orderId);
                });
    }

    // ── Admin CRUD ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> getAll(int page, int size) {
        return PageResponse.of(
                couponRepository.findByDeletedFalse(
                                PageRequest.of(page, size,
                                        Sort.by("createdAt").descending()))
                        .map(couponMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getById(Long id) {
        return couponMapper.toResponse(findById(id));
    }

    @Override
    @Transactional
    public CouponResponse create(CreateCouponRequest req) {
        String code = req.getCode().toUpperCase();
        if (couponRepository.existsByCodeAndDeletedFalse(code)) {
            throw new DuplicateResourceException("Coupon code already exists: " + code);
        }

        validateCreateRequest(req);

        Coupon coupon = couponMapper.toEntity(req);
        coupon.setCode(code);

        if (req.getAllowedUserIds() != null && !req.getAllowedUserIds().isEmpty()) {
            Set<User> users = req.getAllowedUserIds().stream()
                    .map(uid -> userRepository.findById(uid)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("User", "id", uid)))
                    .collect(Collectors.toSet());
            coupon.setAllowedUsers(users);
        }

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon created: {}", saved.getCode());
        return couponMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CouponResponse update(Long id, CreateCouponRequest req) {
        Coupon coupon = findById(id);
        String code   = req.getCode().toUpperCase();

        if (couponRepository.existsByCodeAndIdNotAndDeletedFalse(code, id)) {
            throw new DuplicateResourceException("Coupon code already exists: " + code);
        }

        validateCreateRequest(req);
        couponMapper.updateFromRequest(req, coupon);
        coupon.setCode(code);

        if (req.getAllowedUserIds() != null) {
            Set<User> users = req.getAllowedUserIds().stream()
                    .map(uid -> userRepository.findById(uid)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException("User", "id", uid)))
                    .collect(Collectors.toSet());
            coupon.setAllowedUsers(users);
        }

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Coupon coupon = findById(id);
        coupon.softDelete();
        coupon.setActive(false);
        couponRepository.save(coupon);
        log.info("Coupon deleted: {}", coupon.getCode());
    }

    @Override
    @Transactional
    public CouponResponse toggleActive(Long id) {
        Coupon coupon = findById(id);
        if (!coupon.isActive() && coupon.isExpired()) {
            throw new BadRequestException("Cannot activate an expired coupon");
        }
        coupon.setActive(!coupon.isActive());
        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 * * * *")   // every hour
    public void deactivateExpired() {
        int count = couponRepository.deactivateExpired(LocalDateTime.now());
        if (count > 0) log.info("Deactivated {} expired coupons", count);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private void validateCoupon(Coupon coupon, Long userId, ApplyCouponRequest req) {

        if (!coupon.isActive()) {
            throw new BadRequestException("Coupon is not active");
        }
        if (!coupon.isStarted()) {
            throw new BadRequestException("Coupon is not yet valid");
        }
        if (coupon.isExpired()) {
            throw new BadRequestException("Coupon has expired");
        }
        if (coupon.isUsageLimitReached()) {
            throw new BadRequestException("Coupon usage limit has been reached");
        }

        // Minimum order check
        if (req.getOrderAmount().compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BadRequestException(
                    "Minimum order amount of ₹"
                            + coupon.getMinOrderAmount() + " required for this coupon");
        }

        // Category restriction
        if (coupon.getApplicableCategoryId() != null
                && !coupon.getApplicableCategoryId().equals(req.getCategoryId())) {
            throw new BadRequestException(
                    "Coupon is not applicable for this category");
        }

        // User-specific check
        if (!coupon.isPublic()) {
            boolean allowed = coupon.getAllowedUsers()
                    .stream().anyMatch(u -> u.getId().equals(userId));
            if (!allowed) {
                throw new BadRequestException("Coupon is not valid for your account");
            }
        }

        // Per-user usage limit
        if (coupon.getMaxUsagePerUser() != null && userId != null) {
            long userUsage = usageRepository
                    .countByCouponIdAndUserId(coupon.getId(), userId);
            if (userUsage >= coupon.getMaxUsagePerUser()) {
                throw new BadRequestException(
                        "You have already used this coupon the maximum number of times");
            }
        }
    }

    // ── Discount calculation ──────────────────────────────────────────────────

    private ApplyCouponResponse calculateDiscount(Coupon coupon,
                                                  BigDecimal orderAmount) {
        return switch (coupon.getDiscountType()) {
            case PERCENTAGE    -> calcPercentage(coupon, orderAmount);
            case FLAT          -> calcFlat(coupon, orderAmount);
            case FREE_SHIPPING -> calcFreeShipping(coupon, orderAmount);
            case BUY_X_GET_Y   -> calcBuyXGetY(coupon, orderAmount);
        };
    }

    private ApplyCouponResponse calcPercentage(Coupon coupon, BigDecimal amount) {
        BigDecimal raw = amount
                .multiply(coupon.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal discount = coupon.getMaxDiscountAmount() != null
                ? raw.min(coupon.getMaxDiscountAmount())
                : raw;

        BigDecimal finalAmount = amount.subtract(discount).max(BigDecimal.ZERO);

        return ApplyCouponResponse.builder()
                .couponCode(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .originalAmount(amount)
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .message(coupon.getDiscountValue().stripTrailingZeros().toPlainString()
                        + "% discount applied"
                        + (coupon.getMaxDiscountAmount() != null
                        ? " (capped at ₹" + coupon.getMaxDiscountAmount() + ")" : ""))
                .build();
    }

    private ApplyCouponResponse calcFlat(Coupon coupon, BigDecimal amount) {
        BigDecimal discount    = coupon.getDiscountValue().min(amount);
        BigDecimal finalAmount = amount.subtract(discount).max(BigDecimal.ZERO);

        return ApplyCouponResponse.builder()
                .couponCode(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .originalAmount(amount)
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .message("₹" + discount.stripTrailingZeros().toPlainString()
                        + " flat discount applied")
                .build();
    }

    private ApplyCouponResponse calcFreeShipping(Coupon coupon, BigDecimal amount) {
        // Shipping fee resolved at order time — return 0 discount marker
        return ApplyCouponResponse.builder()
                .couponCode(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .originalAmount(amount)
                .discountAmount(BigDecimal.ZERO)   // actual deduction applied at checkout
                .finalAmount(amount)
                .message("Free shipping applied at checkout")
                .build();
    }

    private ApplyCouponResponse calcBuyXGetY(Coupon coupon, BigDecimal amount) {
        // Quantity logic resolved at checkout — return the rule details
        return ApplyCouponResponse.builder()
                .couponCode(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .originalAmount(amount)
                .discountAmount(BigDecimal.ZERO)   // resolved per-item at checkout
                .finalAmount(amount)
                .freeQuantity(coupon.getGetQuantity())
                .freeProductId(coupon.getGetProductId())
                .message("Buy " + coupon.getBuyQuantity()
                        + " get " + coupon.getGetQuantity() + " free applied")
                .build();
    }

    // ── Request validation ────────────────────────────────────────────────────

    private void validateCreateRequest(CreateCouponRequest req) {
        if (req.getExpiresAt().isBefore(req.getStartsAt())) {
            throw new BadRequestException("Expiry date must be after start date");
        }
        if (req.getDiscountType() == Coupon.DiscountType.PERCENTAGE
                || req.getDiscountType() == Coupon.DiscountType.FLAT) {
            if (req.getDiscountValue() == null) {
                throw new BadRequestException("Discount value is required for "
                        + req.getDiscountType() + " type");
            }
        }
        if (req.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            if (req.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BadRequestException("Percentage discount cannot exceed 100%");
            }
        }
        if (req.getDiscountType() == Coupon.DiscountType.BUY_X_GET_Y) {
            if (req.getBuyQuantity() == null || req.getGetQuantity() == null) {
                throw new BadRequestException(
                        "Buy quantity and get quantity are required for BUY_X_GET_Y");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Coupon findById(Long id) {
        return couponRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    }
}