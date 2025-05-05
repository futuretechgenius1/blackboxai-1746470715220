package com.billing.service.impl;

import com.billing.exception.ResourceNotFoundException;
import com.billing.model.Product;
import com.billing.repository.ProductRepository;
import com.billing.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private static final Integer DEFAULT_LOW_STOCK_THRESHOLD = 10;

    @Override
    public Product createProduct(Product product) {
        if (!isProductNameUnique(product.getName())) {
            throw new IllegalArgumentException("Product name already exists");
        }
        
        if (!validateHsnCode(product.getHsnCode())) {
            throw new IllegalArgumentException("Invalid HSN code format");
        }
        
        if (!validateGstRate(product.getGstRate())) {
            throw new IllegalArgumentException("Invalid GST rate");
        }
        
        product.setActive(true);
        return productRepository.save(product);
    }

    @Override
    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);
        
        if (!product.getName().equals(productDetails.getName()) && 
            !isProductNameUnique(productDetails.getName())) {
            throw new IllegalArgumentException("Product name already exists");
        }
        
        if (!validateHsnCode(productDetails.getHsnCode())) {
            throw new IllegalArgumentException("Invalid HSN code format");
        }
        
        if (!validateGstRate(productDetails.getGstRate())) {
            throw new IllegalArgumentException("Invalid GST rate");
        }
        
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setHsnCode(productDetails.getHsnCode());
        product.setPrice(productDetails.getPrice());
        product.setGstRate(productDetails.getGstRate());
        product.setUnit(productDetails.getUnit());
        product.setStock(productDetails.getStock());
        
        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);  // Soft delete
        productRepository.save(product);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public Page<Product> searchProducts(String searchTerm, Pageable pageable) {
        return productRepository.searchProducts(searchTerm, pageable);
    }

    @Override
    public List<Product> getProductsByHsnCode(String hsnCode) {
        return productRepository.findByHsnCodeAndActiveTrue(hsnCode);
    }

    @Override
    public List<Product> getProductsByGstRate(BigDecimal gstRate) {
        return productRepository.findByGstRateAndActiveTrue(gstRate.doubleValue());
    }

    @Override
    public void updateStock(Long productId, Integer quantity) {
        Product product = getProductById(productId);
        product.setStock(product.getStock() + quantity);
        if (product.getStock() < 0) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        productRepository.save(product);
    }

    @Override
    public List<Product> getLowStockProducts(Integer threshold) {
        return productRepository.findByActiveTrueAndStockLessThan(
            threshold != null ? threshold : DEFAULT_LOW_STOCK_THRESHOLD
        );
    }

    @Override
    public void activateProduct(Long productId) {
        Product product = getProductById(productId);
        product.setActive(true);
        productRepository.save(product);
    }

    @Override
    public void deactivateProduct(Long productId) {
        Product product = getProductById(productId);
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    @Override
    public boolean isProductNameUnique(String name) {
        return !productRepository.existsByNameAndActiveTrue(name);
    }

    @Override
    public boolean validateHsnCode(String hsnCode) {
        // HSN code should be 4-8 digits
        return hsnCode != null && hsnCode.matches("\\d{4,8}");
    }

    @Override
    public boolean validateGstRate(BigDecimal gstRate) {
        // GST rates in India are typically 0%, 5%, 12%, 18%, or 28%
        if (gstRate == null) {
            return false;
        }
        
        BigDecimal[] validRates = {
            BigDecimal.ZERO,
            new BigDecimal("5"),
            new BigDecimal("12"),
            new BigDecimal("18"),
            new BigDecimal("28")
        };
        
        for (BigDecimal validRate : validRates) {
            if (validRate.compareTo(gstRate) == 0) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public BigDecimal calculateGstAmount(Long productId, Integer quantity) {
        Product product = getProductById(productId);
        BigDecimal baseAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        return baseAmount.multiply(product.getGstRate()).divide(BigDecimal.valueOf(100));
    }

    @Override
    public BigDecimal calculateTotalAmount(Long productId, Integer quantity) {
        Product product = getProductById(productId);
        BigDecimal baseAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal gstAmount = calculateGstAmount(productId, quantity);
        return baseAmount.add(gstAmount);
    }
}
