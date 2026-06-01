package com.jullyscraft.repository;

import com.jullyscraft.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ── Lookup ────────────────────────────────────────────────────────────────
    Optional<Category> findBySlugAndDeletedFalse(String slug);
    boolean existsBySlugAndDeletedFalse(String slug);
    boolean existsBySlugAndIdNotAndDeletedFalse(String slug, Long id);
    boolean existsByNameAndDeletedFalse(String name);
    boolean existsByNameAndIdNotAndDeletedFalse(String name, Long id);

    // ── Tree queries ──────────────────────────────────────────────────────────

    // All root categories (no parent), not deleted
    // ✅ LEFT JOIN FETCH children — prevents LazyInitializationException in mapper
    @Query("SELECT DISTINCT c FROM Category c " +
            "LEFT JOIN FETCH c.children ch " +
            "WHERE c.parent IS NULL AND c.deleted = false " +
            "ORDER BY c.displayOrder ASC")
    List<Category> findAllRootCategories();

    // Root categories that are active — with children eagerly loaded
    @Query("SELECT DISTINCT c FROM Category c " +
            "LEFT JOIN FETCH c.children ch " +
            "WHERE c.parent IS NULL AND c.active = true AND c.deleted = false " +
            "ORDER BY c.displayOrder ASC")
    List<Category> findActiveRootCategories();

    // Children of a given parent
    @Query("SELECT c FROM Category c " +
            "WHERE c.parent.id = :parentId AND c.deleted = false " +
            "ORDER BY c.displayOrder ASC")
    List<Category> findChildrenByParentId(Long parentId);

    // Active children of a given parent
    @Query("SELECT c FROM Category c " +
            "WHERE c.parent.id = :parentId AND c.active = true AND c.deleted = false " +
            "ORDER BY c.displayOrder ASC")
    List<Category> findActiveChildrenByParentId(Long parentId);

    // Featured root categories — with children eagerly loaded
    @Query("SELECT DISTINCT c FROM Category c " +
            "LEFT JOIN FETCH c.children ch " +
            "WHERE c.featured = true AND c.active = true AND c.deleted = false " +
            "ORDER BY c.displayOrder ASC")
    List<Category> findFeaturedCategories();

    // Check if category has active children
    @Query("SELECT COUNT(c) > 0 FROM Category c " +
            "WHERE c.parent.id = :parentId AND c.deleted = false")
    boolean hasChildren(Long parentId);

    // All active categories flat (for dropdowns) — no children needed here
    @Query("SELECT c FROM Category c " +
            "WHERE c.active = true AND c.deleted = false " +
            "ORDER BY c.name ASC")
    List<Category> findAllActiveFlat();
}