package com.jullyscraft.repository;

import com.jullyscraft.entity.AffiliatePartner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AffiliatePartnerRepository
        extends JpaRepository<AffiliatePartner, Long> {

    Optional<AffiliatePartner> findByAffiliateCode(String code);

    Optional<AffiliatePartner> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<AffiliatePartner> findByStatus(
            AffiliatePartner.AffiliateStatus status, Pageable pageable);
}