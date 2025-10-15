package com.taxflow.infrastructure.repository;

import com.taxflow.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    Page<Product> findByBusinessId(UUID businessId, Pageable pageable);
    Optional<Product> findByBusinessIdAndSkuIgnoreCase(UUID businessId, String sku);
    List<Product> findTop10ByBusinessIdAndNameContainingIgnoreCaseOrderByName(UUID businessId, String name);

    @Query("select p from Product p where p.business.id = :businessId and p.stock <= p.lowStockThreshold order by p.stock asc")
    List<Product> findLowStock(UUID businessId);

    @Query("select p from Product p where p.business.id = :businessId and p.expiryDate is not null and p.expiryDate <= :date order by p.expiryDate asc")
    List<Product> findExpiringBefore(UUID businessId, LocalDate date);

    @Query("select coalesce(sum(p.stock * p.purchasePrice), 0) from Product p where p.business.id = :businessId")
    BigDecimal stockValue(UUID businessId);
}
