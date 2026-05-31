package com.jullyscraft.repository;

import com.jullyscraft.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository
        extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findByUserId(Long userId, Pageable pageable);

    Page<NotificationLog> findByStatus(
            NotificationLog.DeliveryStatus status, Pageable pageable);

    List<NotificationLog> findByStatusAndRetryCountLessThan(
            NotificationLog.DeliveryStatus status, int maxRetries);

    long countByUserIdAndStatus(Long userId,
                                NotificationLog.DeliveryStatus status);

    @Query("""
        SELECT n FROM NotificationLog n
        WHERE n.user.id = :userId
          AND n.status  = com.jullyscraft.entity.NotificationLog$DeliveryStatus.SENT
        ORDER BY n.createdAt DESC
        """)
    Page<NotificationLog> findSentByUser(Long userId, Pageable pageable);
}