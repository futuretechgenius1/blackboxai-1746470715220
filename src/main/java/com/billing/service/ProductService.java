package com.billing.service;

import com.billing.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    // Basic CRUD operations
    Product createProduct(Product product);
    Product updateProduct(Long id, Product product);
    void deleteProduct(Long id);
    Product getProductById(Long id);
    List<Product> getAllProducts();
    Page<Product> getAllProducts(Pageable pageable);
    
    // Search and filter operations
    Page<Product> searchProducts(String searchTerm, Pageable pageable);
    List<Product> getProductsByHsnCode(String hsnCode);
    List<Product> getProductsByGstRate(BigDecimal gstRate);
    
    // Stock management
    void updateStock(Long productId, Integer quantity);
    List<Product> getLowStockProducts(Integer threshold);
    
    // Product status management
    void activateProduct(Long productId);
    void deactivateProduct(Long productId);
    List<Product> getActiveProducts();
    
    // Validation methods
    boolean isProductNameUnique(String name);
    boolean validateHsnCode(String hsnCode);
    boolean validateGstRate(BigDecimal gstRate);
    
    // GST related operations
    BigDecimal calculateGstAmount(Long productId, Integer quantity);
    BigDecimal calculateTotalAmount(Long productId, Integer quantity);
}
