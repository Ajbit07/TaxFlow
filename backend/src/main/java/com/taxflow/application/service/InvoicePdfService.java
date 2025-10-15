package com.taxflow.application.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.model.Business;
import com.taxflow.domain.model.Customer;
import com.taxflow.domain.model.Invoice;
import com.taxflow.domain.model.InvoiceLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generates a professional GST invoice PDF. CGST/SGST is shown for intra-state
 * supplies and IGST for inter-state supplies, determined by comparing the state
 * code prefix of the business and customer GSTINs when both are available.
 */
@Service
@RequiredArgsConstructor
public class InvoicePdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final Color BRAND = new Color(30, 64, 175);
    private static final Color LIGHT = new Color(239, 246, 255);

    private final InvoiceService invoiceService;
    private final BusinessService businessService;

    @Transactional(readOnly = true)
    public byte[] generate(UUID businessId, UUID invoiceId) {
        Business business = businessService.business(businessId, TaskScope.INVOICE);
        Invoice invoice = invoiceService.require(businessId, invoiceId);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document pdf = new com.lowagie.text.Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(pdf, out);
            pdf.open();
            addHeader(pdf, business, invoice);
            addParties(pdf, business, invoice);
            addLines(pdf, invoice);
            addTotals(pdf, business, invoice);
            addFooter(pdf, invoice);
            pdf.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate invoice PDF", ex);
        }
    }

    private void addHeader(com.lowagie.text.Document pdf, Business business, Invoice invoice) throws Exception {
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BRAND);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        PdfPTable header = new PdfPTable(new float[]{3, 2});
        header.setWidthPercentage(100);
        PdfPCell left = borderless();
        left.addElement(new Paragraph(business.getBusinessName(), title));
        left.addElement(new Paragraph(business.getAddress(), small));
        left.addElement(new Paragraph("Phone: " + business.getPhone() + "  Email: " + business.getEmail(), small));
        if (business.getGstin() != null && !business.getGstin().isBlank()) {
            left.addElement(new Paragraph("GSTIN: " + business.getGstin(), small));
        }
        left.addElement(new Paragraph("PAN: " + business.getPan(), small));
        PdfPCell right = borderless();
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Font invoiceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);
        right.addElement(align(new Paragraph("TAX INVOICE", invoiceFont), Element.ALIGN_RIGHT));
        right.addElement(align(new Paragraph("Invoice #: " + invoice.getInvoiceNumber(), small), Element.ALIGN_RIGHT));
        right.addElement(align(new Paragraph("Date: " + invoice.getInvoiceDate().format(DATE), small), Element.ALIGN_RIGHT));
        if (invoice.getDueDate() != null) {
            right.addElement(align(new Paragraph("Due: " + invoice.getDueDate().format(DATE), small), Element.ALIGN_RIGHT));
        }
        right.addElement(align(new Paragraph("Status: " + invoice.getStatus().name(), small), Element.ALIGN_RIGHT));
        header.addCell(left);
        header.addCell(right);
        pdf.add(header);
        pdf.add(Chunk.NEWLINE);
    }

    private void addParties(com.lowagie.text.Document pdf, Business business, Invoice invoice) throws Exception {
        Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BRAND);
        Font value = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        PdfPCell billTo = boxed();
        billTo.addElement(new Paragraph("Bill To", label));
        Customer customer = invoice.getCustomer();
        if (customer == null) {
            billTo.addElement(new Paragraph("Walk-in customer", value));
        } else {
            billTo.addElement(new Paragraph(customer.getName(), value));
            if (customer.getAddress() != null) {
                billTo.addElement(new Paragraph(customer.getAddress(), value));
            }
            if (customer.getGstin() != null && !customer.getGstin().isBlank()) {
                billTo.addElement(new Paragraph("GSTIN: " + customer.getGstin(), value));
            }
            if (customer.getPhone() != null) {
                billTo.addElement(new Paragraph("Phone: " + customer.getPhone(), value));
            }
        }
        PdfPCell supply = boxed();
        supply.addElement(new Paragraph("Place of Supply", label));
        supply.addElement(new Paragraph(business.getState(), value));
        supply.addElement(new Paragraph("Supply type: " + (interState(business, customer) ? "Inter-state (IGST)" : "Intra-state (CGST + SGST)"), value));
        parties.addCell(billTo);
        parties.addCell(supply);
        pdf.add(parties);
        pdf.add(Chunk.NEWLINE);
    }

    private void addLines(com.lowagie.text.Document pdf, Invoice invoice) throws Exception {
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
        PdfPTable table = new PdfPTable(new float[]{4, 1.4f, 1.2f, 1.6f, 1.2f, 1.8f, 1.8f});
        table.setWidthPercentage(100);
        for (String head : new String[]{"Description", "HSN/SAC", "Qty", "Rate", "GST %", "Taxable", "Total"}) {
            PdfPCell cell = new PdfPCell(new Phrase(head, headFont));
            cell.setBackgroundColor(BRAND);
            cell.setPadding(6);
            table.addCell(cell);
        }
        boolean shade = false;
        for (InvoiceLine line : invoice.getLines()) {
            Color background = shade ? LIGHT : Color.WHITE;
            table.addCell(cell(line.getDescription(), cellFont, background, Element.ALIGN_LEFT));
            table.addCell(cell(line.getHsnCode(), cellFont, background, Element.ALIGN_CENTER));
            table.addCell(cell(line.getQuantity().stripTrailingZeros().toPlainString(), cellFont, background, Element.ALIGN_RIGHT));
            table.addCell(cell(money(line.getUnitPrice()), cellFont, background, Element.ALIGN_RIGHT));
            table.addCell(cell(line.getGstRate().stripTrailingZeros().toPlainString() + "%", cellFont, background, Element.ALIGN_RIGHT));
            table.addCell(cell(money(line.getTaxableAmount()), cellFont, background, Element.ALIGN_RIGHT));
            table.addCell(cell(money(line.getTotalAmount()), cellFont, background, Element.ALIGN_RIGHT));
            shade = !shade;
        }
        pdf.add(table);
    }

    private void addTotals(com.lowagie.text.Document pdf, Business business, Invoice invoice) throws Exception {
        Font label = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        PdfPTable totals = new PdfPTable(new float[]{7, 3});
        totals.setWidthPercentage(100);
        PdfPCell spacer = borderless();
        spacer.addElement(new Paragraph(" "));
        PdfPCell box = boxed();
        box.addElement(totalRow("Subtotal", money(invoice.getSubtotal()), label));
        if (interState(business, invoice.getCustomer())) {
            box.addElement(totalRow("IGST", money(invoice.getTotalGst()), label));
        } else {
            BigDecimal half = invoice.getTotalGst().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            box.addElement(totalRow("CGST", money(half), label));
            box.addElement(totalRow("SGST", money(invoice.getTotalGst().subtract(half)), label));
        }
        box.addElement(totalRow("Grand Total", money(invoice.getTotalAmount()), bold));
        box.addElement(totalRow("Paid", money(invoice.getPaidAmount()), label));
        box.addElement(totalRow("Balance Due", money(invoice.getTotalAmount().subtract(invoice.getPaidAmount())), bold));
        totals.addCell(spacer);
        totals.addCell(box);
        pdf.add(totals);
    }

    private void addFooter(com.lowagie.text.Document pdf, Invoice invoice) throws Exception {
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
        if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
            pdf.add(new Paragraph("Notes: " + invoice.getNotes(), small));
        }
        if (invoice.getQrPayload() != null) {
            pdf.add(new Paragraph("Pay via UPI: " + invoice.getQrPayload(), small));
        }
        pdf.add(new Paragraph("This is a computer generated invoice created with TaxFlow.", small));
    }

    private boolean interState(Business business, Customer customer) {
        if (business.getGstin() == null || business.getGstin().length() < 2
                || customer == null || customer.getGstin() == null || customer.getGstin().length() < 2) {
            return false;
        }
        return !business.getGstin().substring(0, 2).equals(customer.getGstin().substring(0, 2));
    }

    private Paragraph totalRow(String label, String amount, Font font) {
        Paragraph row = new Paragraph(label + ": " + amount, font);
        row.setAlignment(Element.ALIGN_RIGHT);
        return row;
    }

    private String money(BigDecimal value) {
        return "Rs. " + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private Paragraph align(Paragraph paragraph, int alignment) {
        paragraph.setAlignment(alignment);
        return paragraph;
    }

    private PdfPCell cell(String text, Font font, Color background, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell borderless() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell boxed() {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(new Color(203, 213, 225));
        cell.setPadding(8);
        return cell;
    }
}
