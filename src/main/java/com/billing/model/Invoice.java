package com.billing.model;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String invoiceNumber;
    
    @Column(nullable = false)
    private LocalDateTime invoiceDate;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // User who created the invoice
    
    // Customer details
    @Column(nullable = false)
    private String customerName;
    
    @Column(nullable = false)
    private String customerGstin;
    
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;
    
    // Invoice items
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();
    
    // Payment details
    private String paymentMethod;
    private String paymentStatus;
    
    // Calculated fields
    @Column(nullable = false)
    private BigDecimal subTotal = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal cgstAmount = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal sgstAmount = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal igstAmount = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    // Notes
    private String notes;
    
    // Status
    @Column(nullable = false)
    private String status = "DRAFT";  // DRAFT, ISSUED, PAID, CANCELLED
    
    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
        recalculateTotals();
    }
    
    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
        recalculateTotals();
    }
    
    private void recalculateTotals() {
        this.subTotal = BigDecimal.ZERO;
        this.cgstAmount = BigDecimal.ZERO;
        this.sgstAmount = BigDecimal.ZERO;
        this.igstAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        
        for (InvoiceItem item : items) {
            this.subTotal = this.subTotal.add(item.getSubTotal());
            this.cgstAmount = this.cgstAmount.add(item.getCgstAmount());
            this.sgstAmount = this.sgstAmount.add(item.getSgstAmount());
            this.igstAmount = this.igstAmount.add(item.getIgstAmount());
            this.totalAmount = this.totalAmount.add(item.getTotalAmount());
        }
    }
}
