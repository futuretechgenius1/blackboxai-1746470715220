package com.billing.service;

import com.billing.model.Invoice;
import com.billing.model.InvoiceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface InvoiceService {
    // Basic CRUD operations
    Invoice createInvoice(Invoice invoice);
    Invoice updateInvoice(Long id, Invoice invoice);
    void deleteInvoice(Long id);
    Invoice getInvoiceById(Long id);
    Invoice getInvoiceByNumber(String invoiceNumber);
    List<Invoice> getAllInvoices();
    Page<Invoice> getAllInvoices(Pageable pageable);
    
    // Invoice item operations
    Invoice addInvoiceItem(Long invoiceId, InvoiceItem item);
    Invoice updateInvoiceItem(Long invoiceId, Long itemId, InvoiceItem item);
    Invoice removeInvoiceItem(Long invoiceId, Long itemId);
    
    // Invoice status management
    Invoice issueInvoice(Long invoiceId);
    Invoice markAsPaid(Long invoiceId);
    Invoice cancelInvoice(Long invoiceId);
    
    // Search and filter operations
    Page<Invoice> searchInvoices(String searchTerm, Pageable pageable);
    List<Invoice> getInvoicesByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    Page<Invoice> getInvoicesByStatus(String status, Pageable pageable);
    Page<Invoice> getInvoicesByCustomer(String customerName, Pageable pageable);
    
    // GST calculations
    BigDecimal calculateSubTotal(Long invoiceId);
    BigDecimal calculateGstAmount(Long invoiceId);
    BigDecimal calculateTotalAmount(Long invoiceId);
    Map<String, BigDecimal> getGstBreakdown(Long invoiceId); // Returns CGST, SGST, IGST amounts
    
    // Reports and analytics
    Map<String, BigDecimal> getMonthlySalesReport(int year, int month);
    Map<String, BigDecimal> getGstCollectionReport(LocalDateTime startDate, LocalDateTime endDate);
    List<Map<String, Object>> getTopSellingProducts(LocalDateTime startDate, LocalDateTime endDate, int limit);
    
    // Export operations
    byte[] generatePdf(Long invoiceId);
    byte[] exportInvoices(LocalDateTime startDate, LocalDateTime endDate, String format);
    
    // Validation methods
    boolean validateInvoiceNumber(String invoiceNumber);
    boolean validateGstin(String gstin);
}
