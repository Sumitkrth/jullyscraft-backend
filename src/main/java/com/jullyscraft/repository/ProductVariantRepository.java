package com.jullyscraft.repository;

import com.jullyscraft.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndDeletedFalse(Long productId);

    Optional<ProductVariant> findBySkuAndDeletedFalse(String sku);

    boolean existsBySkuAndDeletedFalse(String sku);
    boolean existsBySkuAndIdNotAndDeletedFalse(String sku, Long id);

    Optional<ProductVariant> findByIdAndProductIdAndDeletedFalse(Long id, Long productId);
}