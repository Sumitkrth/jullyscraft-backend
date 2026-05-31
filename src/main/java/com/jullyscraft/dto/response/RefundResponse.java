package com.jullyscraft.dto.response;

import com.jullyscraft.entity.Payment;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RefundResponse {
    private String                 refundId;
    private BigDecimal             refundAmount;
    private Payment.RefundStatus   refundStatus;
    private String                 message;
}