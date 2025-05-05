package com.billing.controller;

import com.billing.model.Invoice;
import com.billing.model.Product;
import com.billing.service.InvoiceService;
import com.billing.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final InvoiceService invoiceService;
    private final ProductService productService;
    private static final int LOW_STOCK_THRESHOLD = 10;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // If not authenticated, redirect to login
        if (userDetails == null) {
            return "redirect:/login";
        }

        // Get current month and previous month dates
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);
        
        LocalDateTime currentMonthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime currentMonthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        LocalDateTime lastMonthStart = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime lastMonthEnd = lastMonth.atEndOfMonth().atTime(23, 59, 59);

        // Calculate statistics
        BigDecimal totalSales = calculateTotalSales(currentMonthStart, currentMonthEnd);
        BigDecimal lastMonthSales = calculateTotalSales(lastMonthStart, lastMonthEnd);
        
        // Calculate growth percentage
        double salesGrowth = 0.0;
        if (lastMonthSales.compareTo(BigDecimal.ZERO) > 0) {
            salesGrowth = totalSales.subtract(lastMonthSales)
                .divide(lastMonthSales, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
        }

        // Get total invoices
        List<Invoice> currentMonthInvoices = invoiceService.getInvoicesByDateRange(
            currentMonthStart, currentMonthEnd);
        int totalInvoices = invoiceService.getAllInvoices().size();
        int invoicesThisMonth = currentMonthInvoices.size();

        // Get products statistics
        List<Product> allProducts = productService.getAllProducts();
        List<Product> lowStockProducts = productService.getLowStockProducts(LOW_STOCK_THRESHOLD);

        // Get GST statistics
        Map<String, BigDecimal> gstReport = invoiceService.getGstCollectionReport(
            currentMonthStart, currentMonthEnd);
        BigDecimal totalGst = gstReport.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get recent invoices (last 5)
        List<Invoice> recentInvoices = invoiceService.getAllInvoices(
            PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "invoiceDate"))
        ).getContent();

        // Add data to model
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("salesGrowth", String.format("%.1f%%", salesGrowth));
        model.addAttribute("totalInvoices", totalInvoices);
        model.addAttribute("invoicesThisMonth", invoicesThisMonth);
        model.addAttribute("totalProducts", allProducts.size());
        model.addAttribute("lowStockProducts", lowStockProducts.size());
        model.addAttribute("totalGst", totalGst);
        model.addAttribute("recentInvoices", recentInvoices);
        model.addAttribute("lowStockProductsList", lowStockProducts);

        return "dashboard";
    }

    private BigDecimal calculateTotalSales(LocalDateTime startDate, LocalDateTime endDate) {
        List<Invoice> invoices = invoiceService.getInvoicesByDateRange(startDate, endDate);
        return invoices.stream()
            .filter(invoice -> "PAID".equals(invoice.getStatus()))
            .map(Invoice::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
