package com.billing.model;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    @Column(nullable = false)
    private String hsnCode;  // HSN code for GST classification
    
    @Column(nullable = false)
    private BigDecimal price;  // Base price without GST
    
    @Column(nullable = false)
    private BigDecimal gstRate;  // GST rate in percentage
    
    private String unit;  // Unit of measurement (e.g., pieces, kg, etc.)
    
    @Column(nullable = false)
    private Integer stock;
    
    @Column(nullable = false)
    private boolean active = true;
    
    // Calculated fields
    @Transient
    public BigDecimal getGstAmount() {
        return price.multiply(gstRate.divide(BigDecimal.valueOf(100)));
    }
    
    @Transient
    public BigDecimal getTotalPrice() {
        return price.add(getGstAmount());
    }
}
