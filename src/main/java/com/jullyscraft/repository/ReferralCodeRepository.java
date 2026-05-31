package com.jullyscraft.repository;

import com.jullyscraft.entity.ReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferralCodeRepository
        extends JpaRepository<ReferralCode, Long> {

    Optional<ReferralCode> findByCodeAndDeletedFalse(String code);

    Optional<ReferralCode> findByOwnerIdAndDeletedFalse(Long ownerId);

    boolean existsByCodeAndDeletedFalse(String code);
}