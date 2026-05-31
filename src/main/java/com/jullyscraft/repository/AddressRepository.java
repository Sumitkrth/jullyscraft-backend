package com.jullyscraft.repository;

import com.jullyscraft.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdAndDeletedFalse(Long userId);

    Optional<Address> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    long countByUserIdAndDeletedFalse(Long userId);

    Optional<Address> findByUserIdAndDefaultAddressTrue(Long userId);

    @Modifying
    @Query("UPDATE Address a SET a.defaultAddress = false WHERE a.user.id = :userId")
    void clearDefaultAddress(Long userId);
}