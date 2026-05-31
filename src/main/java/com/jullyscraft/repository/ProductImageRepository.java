package com.jullyscraft.repository;

import com.jullyscraft.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdAndDeletedFalseOrderByDisplayOrderAsc(Long productId);

    Optional<ProductImage> findByIdAndProductIdAndDeletedFalse(Long id, Long productId);

    @Modifying
    @Query("UPDATE ProductImage i SET i.primaryImage = false WHERE i.product.id = :productId")
    void clearPrimaryImage(Long productId);

    long countByProductIdAndDeletedFalse(Long productId);
}