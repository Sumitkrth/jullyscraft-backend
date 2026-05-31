package com.jullyscraft.repository;

import com.jullyscraft.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :from AND u.createdAt <= :to")
    long countNewUsers(LocalDateTime from, LocalDateTime to);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :time WHERE u.id = :id")
    void updateLastLogin(Long id, LocalDateTime time);

    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.email = :email")
    void markEmailVerified(String email);
}