package com.billing.service.impl;

import com.billing.exception.ResourceNotFoundException;
import com.billing.model.Invoice;
import com.billing.model.InvoiceItem;
import com.billing.model.Product;
import com.billing.repository.InvoiceItemRepository;
import com.billing.repository.InvoiceRepository;
import com.billing.repository.ProductRepository;
import com.billing.service.InvoiceService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ProductRepository productRepository;

    @Override
    public Invoice createInvoice(Invoice invoice) {
        validateInvoice(invoice);
        
        // Generate unique invoice number
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setInvoiceDate(LocalDateTime.now());
        invoice.setStatus("DRAFT");
        
        // Calculate totals
        calculateInvoiceTotals(invoice);
        
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice updateInvoice(Long id, Invoice invoiceDetails) {
        Invoice invoice = getInvoiceById(id);
        
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only draft invoices can be updated");
        }
        
        // Update customer details
        invoice.setCustomerName(invoiceDetails.getCustomerName());
        invoice.setCustomerGstin(invoiceDetails.getCustomerGstin());
        invoice.setCustomerEmail(invoiceDetails.getCustomerEmail());
        invoice.setCustomerPhone(invoiceDetails.getCustomerPhone());
        invoice.setCustomerAddress(invoiceDetails.getCustomerAddress());
        
        // Update items if provided
        if (invoiceDetails.getItems() != null) {
            invoice.getItems().clear();
            invoice.getItems().addAll(invoiceDetails.getItems());
            invoice.getItems().forEach(item -> item.setInvoice(invoice));
        }
        
        calculateInvoiceTotals(invoice);
        return invoiceRepository.save(invoice);
    }

    @Override
    public void deleteInvoice(Long id) {
        Invoice invoice = getInvoiceById(id);
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only draft invoices can be deleted");
        }
        invoiceRepository.delete(invoice);
    }

    @Override
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
    }

    @Override
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", "number", invoiceNumber));
    }

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public Page<Invoice> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    @Override
    public Invoice addInvoiceItem(Long invoiceId, InvoiceItem item) {
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Can only add items to draft invoices");
        }
        
        // Validate and set product details
        Product product = productRepository.findById(item.getProduct().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", item.getProduct().getId()));
            
        if (item.getQuantity() > product.getStock()) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
        }
        
        item.setInvoice(invoice);
        item.setPrice(product.getPrice());
        item.setGstRate(product.getGstRate());
        
        invoice.addItem(item);
        calculateInvoiceTotals(invoice);
        
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice updateInvoiceItem(Long invoiceId, Long itemId, InvoiceItem itemDetails) {
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Can only update items in draft invoices");
        }
        
        InvoiceItem item = invoice.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Invoice Item", "id", itemId));
            
        item.setQuantity(itemDetails.getQuantity());
        calculateInvoiceTotals(invoice);
        
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice removeInvoiceItem(Long invoiceId, Long itemId) {
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Can only remove items from draft invoices");
        }
        
        invoice.getItems().removeIf(item -> item.getId().equals(itemId));
        calculateInvoiceTotals(invoice);
        
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice issueInvoice(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only draft invoices can be issued");
        }
        
        // Update product stock
        for (InvoiceItem item : invoice.getItems()) {
            Product product = item.getProduct();
            if (item.getQuantity() > product.getStock()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }
        
        invoice.setStatus("ISSUED");
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice markAsPaid(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (!"ISSUED".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only issued invoices can be marked as paid");
        }
        
        invoice.setStatus("PAID");
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice cancelInvoice(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        
        if ("CANCELLED".equals(invoice.getStatus())) {
            throw new IllegalStateException("Invoice is already cancelled");
        }
        
        // Restore product stock if invoice was issued
        if ("ISSUED".equals(invoice.getStatus()) || "PAID".equals(invoice.getStatus())) {
            for (InvoiceItem item : invoice.getItems()) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }
        
        invoice.setStatus("CANCELLED");
        return invoiceRepository.save(invoice);
    }

    @Override
    public Page<Invoice> searchInvoices(String searchTerm, Pageable pageable) {
        return invoiceRepository.searchInvoices(searchTerm, pageable);
    }

    @Override
    public List<Invoice> getInvoicesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return invoiceRepository.findByInvoiceDateBetween(startDate, endDate);
    }

    @Override
    public Page<Invoice> getInvoicesByStatus(String status, Pageable pageable) {
        return invoiceRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<Invoice> getInvoicesByCustomer(String customerName, Pageable pageable) {
        return invoiceRepository.findByCustomerNameContainingIgnoreCase(customerName, pageable);
    }

    @Override
    public BigDecimal calculateSubTotal(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        return invoice.getSubTotal();
    }

    @Override
    public BigDecimal calculateGstAmount(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        return invoice.getCgstAmount()
            .add(invoice.getSgstAmount())
            .add(invoice.getIgstAmount());
    }

    @Override
    public BigDecimal calculateTotalAmount(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        return invoice.getTotalAmount();
    }

    @Override
    public Map<String, BigDecimal> getGstBreakdown(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        Map<String, BigDecimal> breakdown = new HashMap<>();
        breakdown.put("CGST", invoice.getCgstAmount());
        breakdown.put("SGST", invoice.getSgstAmount());
        breakdown.put("IGST", invoice.getIgstAmount());
        return breakdown;
    }

    @Override
    public Map<String, BigDecimal> getMonthlySalesReport(int year, int month) {
        // Implementation for monthly sales report
        // TODO: Implement detailed monthly sales report
        return new HashMap<>();
    }

    @Override
    public Map<String, BigDecimal> getGstCollectionReport(LocalDateTime startDate, LocalDateTime endDate) {
        // Implementation for GST collection report
        // TODO: Implement detailed GST collection report
        return new HashMap<>();
    }

    @Override
    public List<Map<String, Object>> getTopSellingProducts(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        // Implementation for top selling products report
        // TODO: Implement top selling products report
        return new ArrayList<>();
    }

    @Override
    public byte[] generatePdf(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        
        try {
            Document document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            
            document.open();
            // Add invoice details to PDF
            // TODO: Implement detailed PDF generation
            document.close();
            
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    @Override
    public byte[] exportInvoices(LocalDateTime startDate, LocalDateTime endDate, String format) {
        // Implementation for exporting invoices
        // TODO: Implement invoice export functionality
        return new byte[0];
    }

    @Override
    public boolean validateInvoiceNumber(String invoiceNumber) {
        // Invoice number format: INV-YYYYMMDD-XXXX
        return invoiceNumber != null && 
               invoiceNumber.matches("INV-\\d{8}-\\d{4}");
    }

    @Override
    public boolean validateGstin(String gstin) {
        // GSTIN format: 2 digits state code + 10 digits PAN + 1 digit entity number + 1 digit check sum
        return gstin != null && 
               gstin.matches("\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}[\\d,A-Z]{1}[Z]{1}[\\d,A-Z]{1}");
    }

    // Helper methods
    private void validateInvoice(Invoice invoice) {
        if (invoice.getCustomerName() == null || invoice.getCustomerName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        
        if (invoice.getCustomerGstin() != null && !validateGstin(invoice.getCustomerGstin())) {
            throw new IllegalArgumentException("Invalid GSTIN format");
        }
    }

    private void calculateInvoiceTotals(Invoice invoice) {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal cgstAmount = BigDecimal.ZERO;
        BigDecimal sgstAmount = BigDecimal.ZERO;
        BigDecimal igstAmount = BigDecimal.ZERO;
        
        for (InvoiceItem item : invoice.getItems()) {
            subTotal = subTotal.add(item.getSubTotal());
            cgstAmount = cgstAmount.add(item.getCgstAmount());
            sgstAmount = sgstAmount.add(item.getSgstAmount());
            igstAmount = igstAmount.add(item.getIgstAmount());
        }
        
        invoice.setSubTotal(subTotal);
        invoice.setCgstAmount(cgstAmount);
        invoice.setSgstAmount(sgstAmount);
        invoice.setIgstAmount(igstAmount);
        invoice.setTotalAmount(subTotal.add(cgstAmount).add(sgstAmount).add(igstAmount));
    }

    private String generateInvoiceNumber() {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Get the count of invoices for today and increment
        long todayInvoiceCount = invoiceRepository.count() + 1;
        
        return String.format("INV-%s-%04d", datePart, todayInvoiceCount);
    }
}
