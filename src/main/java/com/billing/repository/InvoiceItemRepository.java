package com.billing.repository;

import com.billing.model.Invoice;
import com.billing.model.InvoiceItem;
import com.billing.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    // Find items by invoice
    List<InvoiceItem> findByInvoice(Invoice invoice);
    
    // Find items by product
    List<InvoiceItem> findByProduct(Product product);
    
    // Get total quantity sold for a product in a date range
    @Query("SELECT SUM(ii.quantity) FROM InvoiceItem ii " +
           "WHERE ii.product = :product AND " +
           "ii.invoice.status = 'PAID' AND " +
           "ii.invoice.invoiceDate BETWEEN :startDate AND :endDate")
    Integer getTotalQuantitySold(@Param("product") Product product,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
    
    // Get top selling products
    @Query("SELECT ii.product, SUM(ii.quantity) as totalQuantity " +
           "FROM InvoiceItem ii " +
           "WHERE ii.invoice.status = 'PAID' AND " +
           "ii.invoice.invoiceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ii.product " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> getTopSellingProducts(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
    
    // Get total GST collected per product
    @Query("SELECT ii.product, " +
           "SUM(ii.cgstAmount) as totalCgst, " +
           "SUM(ii.sgstAmount) as totalSgst, " +
           "SUM(ii.igstAmount) as totalIgst " +
           "FROM InvoiceItem ii " +
           "WHERE ii.invoice.status = 'PAID' AND " +
           "ii.invoice.invoiceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ii.product")
    List<Object[]> getGstCollectedPerProduct(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
}
