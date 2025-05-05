package com.billing.repository;

import com.billing.model.Invoice;
import com.billing.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    // Find by invoice number
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    
    // Find invoices by user
    Page<Invoice> findByUser(User user, Pageable pageable);
    
    // Find invoices by status
    Page<Invoice> findByStatus(String status, Pageable pageable);
    
    // Find invoices by date range
    List<Invoice> findByInvoiceDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find invoices by customer
    Page<Invoice> findByCustomerNameContainingIgnoreCase(String customerName, Pageable pageable);
    
    // Search invoices
    @Query("SELECT i FROM Invoice i WHERE " +
           "LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.customerGstin) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Invoice> searchInvoices(@Param("search") String search, Pageable pageable);
    
    // Get total sales amount for a date range
    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE " +
           "i.status = 'PAID' AND i.invoiceDate BETWEEN :startDate AND :endDate")
    Double getTotalSalesAmount(@Param("startDate") LocalDateTime startDate, 
                             @Param("endDate") LocalDateTime endDate);
    
    // Get total GST collected for a date range
    @Query("SELECT SUM(i.cgstAmount + i.sgstAmount + i.igstAmount) FROM Invoice i WHERE " +
           "i.status = 'PAID' AND i.invoiceDate BETWEEN :startDate AND :endDate")
    Double getTotalGstCollected(@Param("startDate") LocalDateTime startDate, 
                              @Param("endDate") LocalDateTime endDate);
    
    // Get monthly sales report
    @Query("SELECT FUNCTION('MONTH', i.invoiceDate) as month, " +
           "SUM(i.totalAmount) as totalAmount, " +
           "SUM(i.cgstAmount + i.sgstAmount + i.igstAmount) as totalGst " +
           "FROM Invoice i WHERE i.status = 'PAID' AND " +
           "i.invoiceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('MONTH', i.invoiceDate)")
    List<Object[]> getMonthlySalesReport(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
}
