package com.jullyscraft.repository;

import com.jullyscraft.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    boolean existsByGatewayPaymentIdAndStatus(
            String gatewayPaymentId, Payment.PaymentStatus status);

    // Latest captured payment for an order
    Optional<Payment> findTopByOrderIdAndStatusOrderByCreatedAtDesc(
            Long orderId, Payment.PaymentStatus status);
}