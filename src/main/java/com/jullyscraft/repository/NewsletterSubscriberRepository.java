package com.jullyscraft.repository;

import com.jullyscraft.entity.NewsletterSubscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsletterSubscriberRepository
        extends JpaRepository<NewsletterSubscriber, Long> {

    boolean existsByEmailAndDeletedFalse(String email);

    Optional<NewsletterSubscriber> findByEmailAndDeletedFalse(String email);

    Optional<NewsletterSubscriber> findByUnsubscribeToken(String token);

    Page<NewsletterSubscriber> findByActiveTrueAndDeletedFalse(Pageable pageable);

    long countByActiveTrueAndDeletedFalse();

    @Modifying
    @Query("UPDATE NewsletterSubscriber n SET n.active = false, " +
            "n.unsubscribedAt = CURRENT_TIMESTAMP WHERE n.email = :email")
    void unsubscribe(String email);
}