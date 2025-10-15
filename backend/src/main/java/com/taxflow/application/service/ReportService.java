package com.taxflow.application.service;

import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.taxflow.application.dto.TaxFlowDtos.GstSummary;
import com.taxflow.domain.enums.InvoiceType;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.model.Expense;
import com.taxflow.domain.model.Invoice;
import com.taxflow.domain.model.Product;
import com.taxflow.infrastructure.repository.ExpenseRepository;
import com.taxflow.infrastructure.repository.InvoiceRepository;
import com.taxflow.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final BusinessService businessService;
    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final ProductRepository productRepository;
    private final TaxEngineService taxEngineService;

    @Transactional(readOnly = true)
    public byte[] pdf(UUID businessId, String reportType, LocalDate from, LocalDate to) {
        businessService.business(businessId, TaskScope.REPORT);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document pdf = new com.lowagie.text.Document();
            PdfWriter.getInstance(pdf, out);
            pdf.open();
            pdf.add(new Paragraph("TaxFlow " + reportType + " Report"));
            pdf.add(new Paragraph("Period: " + from + " to " + to));
            PdfPTable table = new PdfPTable(3);
            table.addCell("Metric");
            table.addCell("Value");
            table.addCell("Notes");
            addRows(table, businessId, reportType, from, to);
            pdf.add(table);
            pdf.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate PDF report", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] excel(UUID businessId, String reportType, LocalDate from, LocalDate to) {
        businessService.business(businessId, TaskScope.REPORT);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(reportType);
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Metric");
            header.createCell(1).setCellValue("Value");
            header.createCell(2).setCellValue("Notes");
            List<String[]> rows = rowData(businessId, reportType, from, to);
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(rows.get(i)[0]);
                row.createCell(1).setCellValue(rows.get(i)[1]);
                row.createCell(2).setCellValue(rows.get(i)[2]);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate Excel report", ex);
        }
    }

    private void addRows(PdfPTable table, UUID businessId, String type, LocalDate from, LocalDate to) {
        rowData(businessId, type, from, to).forEach(row -> {
            table.addCell(row[0]);
            table.addCell(row[1]);
            table.addCell(row[2]);
        });
    }

    private List<String[]> rowData(UUID businessId, String type, LocalDate from, LocalDate to) {
        String normalized = type == null ? "monthly" : type.toLowerCase();
        if (normalized.contains("gst")) {
            GstSummary gst = taxEngineService.gstSummary(businessId, from, to);
            return List.of(
                    row("Output GST", gst.outputGst(), "Sales tax"),
                    row("Input GST", gst.inputGst(), "ITC"),
                    row("Net payable", gst.netGstPayable(), "GST due"));
        }
        if (normalized.contains("inventory")) {
            BigDecimal value = productRepository.stockValue(businessId);
            List<Product> low = productRepository.findLowStock(businessId);
            return List.of(row("Stock value", value, "Current inventory valuation"), row("Low stock products", BigDecimal.valueOf(low.size()), "Alerts"));
        }
        if (normalized.contains("expense")) {
            BigDecimal total = expenseRepository.sumAmount(businessId, from, to);
            List<Expense> latest = expenseRepository.findTop10ByBusinessIdOrderByExpenseDateDesc(businessId);
            return List.of(row("Expenses", total, "Total spend"), row("Recent expenses", BigDecimal.valueOf(latest.size()), "Latest records"));
        }
        BigDecimal sales = invoiceRepository.sumTotal(businessId, InvoiceType.GST_INVOICE, from, to);
        BigDecimal purchases = invoiceRepository.sumTotal(businessId, InvoiceType.PURCHASE_BILL, from, to);
        List<Invoice> invoices = invoiceRepository.findForPeriod(businessId, from, to);
        return List.of(row("Sales", sales, "GST invoices"), row("Purchases", purchases, "Purchase register"), row("Invoices", BigDecimal.valueOf(invoices.size()), "Transactions"));
    }

    private String[] row(String label, BigDecimal value, String note) {
        return new String[]{label, value.toPlainString(), note};
    }
}
