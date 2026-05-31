package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_subscribers", indexes = {
        @Index(name = "idx_newsletter_email", columnList = "email", unique = true),
        @Index(name = "idx_newsletter_active", columnList = "active")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NewsletterSubscriber extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 100)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 100)
    private String source;              // HOME_PAGE | CHECKOUT | POPUP | API

    @Column(length = 100)
    private String unsubscribeToken;

    @Column
    private LocalDateTime subscribedAt;

    @Column
    private LocalDateTime unsubscribedAt;

    // Optional link to registered user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}