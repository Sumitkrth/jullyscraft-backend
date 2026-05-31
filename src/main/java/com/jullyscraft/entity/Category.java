package com.jullyscraft.entity;

import com.jullyscraft.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_slug",   columnList = "slug",      unique = true),
        @Index(name = "idx_category_parent", columnList = "parent_id")
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Category extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    // ── Self-referencing parent/children ─────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    // ── Flags ─────────────────────────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    // ── Derived — updated by service on product save/delete ───────────────────
    @Column(nullable = false)
    @Builder.Default
    private long productCount = 0;

    // ── Helpers ───────────────────────────────────────────────────────────────
    public boolean isRootCategory() {
        return parent == null;
    }

    public void incrementProductCount() { this.productCount++; }
    public void decrementProductCount() {
        if (this.productCount > 0) this.productCount--;
    }
}