package com.jullyscraft.repository;

import com.jullyscraft.entity.Shipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrderId(Long orderId);

    Optional<Shipment> findByAwbNumber(String awbNumber);

    Optional<Shipment> findByProviderShipmentId(String providerShipmentId);

    Page<Shipment> findByProvider(Shipment.ShippingProvider provider, Pageable pageable);

    Page<Shipment> findByStatus(Shipment.ShipmentStatus status, Pageable pageable);

    boolean existsByOrderId(Long orderId);
}