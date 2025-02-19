package com.sparta.delivery.domain.product.repository;

import com.sparta.delivery.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findAllByOrderByCreatedAtDescUpdatedAtDesc(Pageable pageable);

    boolean existsByNameAndStore_StoreIdAndDeletedAtIsNull(String name, UUID storeId);
}
