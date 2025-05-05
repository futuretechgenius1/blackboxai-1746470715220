package com.billing.model;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private BigDecimal price;  // Price per unit
    
    @Column(nullable = false)
    private BigDecimal gstRate;  // GST rate in percentage
    
    // Calculated fields
    @Column(nullable = false)
    private BigDecimal subTotal;  // quantity * price
    
    @Column(nullable = false)
    private BigDecimal cgstRate = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal sgstRate = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal igstRate = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal cgstAmount = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal sgstAmount = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal igstAmount = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal totalAmount;  // subTotal + GST amounts
    
    // Helper methods for calculations
    @PrePersist
    @PreUpdate
    protected void calculateAmounts() {
        // Calculate subtotal
        this.subTotal = this.price.multiply(BigDecimal.valueOf(this.quantity));
        
        // Set GST rates based on whether it's inter-state (IGST) or intra-state (CGST + SGST)
        if (this.igstRate.compareTo(BigDecimal.ZERO) > 0) {
            // Inter-state: Only IGST applies
            this.igstAmount = this.subTotal.multiply(this.igstRate.divide(BigDecimal.valueOf(100)));
            this.cgstAmount = BigDecimal.ZERO;
            this.sgstAmount = BigDecimal.ZERO;
        } else {
            // Intra-state: CGST + SGST applies
            this.cgstAmount = this.subTotal.multiply(this.cgstRate.divide(BigDecimal.valueOf(100)));
            this.sgstAmount = this.subTotal.multiply(this.sgstRate.divide(BigDecimal.valueOf(100)));
            this.igstAmount = BigDecimal.ZERO;
        }
        
        // Calculate total amount
        this.totalAmount = this.subTotal.add(this.cgstAmount).add(this.sgstAmount).add(this.igstAmount);
    }
    
    // Helper methods to get total GST amount
    @Transient
    public BigDecimal getTotalGstAmount() {
        return this.cgstAmount.add(this.sgstAmount).add(this.igstAmount);
    }
}
