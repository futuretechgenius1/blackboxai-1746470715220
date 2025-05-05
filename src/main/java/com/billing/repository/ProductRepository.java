package com.billing.repository;

import com.billing.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // Find active products
    List<Product> findByActiveTrue();
    Page<Product> findByActiveTrue(Pageable pageable);
    
    // Search products by name or HSN code
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "p.hsnCode LIKE CONCAT('%', :search, '%'))")
    Page<Product> searchProducts(@Param("search") String search, Pageable pageable);
    
    // Find products with low stock
    List<Product> findByActiveTrueAndStockLessThan(Integer minStock);
    
    // Find products by HSN code
    List<Product> findByHsnCodeAndActiveTrue(String hsnCode);
    
    // Find products by GST rate
    List<Product> findByGstRateAndActiveTrue(Double gstRate);
    
    // Check if product exists by name
    boolean existsByNameAndActiveTrue(String name);
}
