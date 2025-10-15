package com.taxflow.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxflow.application.dto.TaxFlowDtos.ComplianceIssue;
import com.taxflow.application.dto.TaxFlowDtos.DashboardResponse;
import com.taxflow.application.dto.TaxFlowDtos.FilingResponse;
import com.taxflow.application.dto.TaxFlowDtos.FilingWizardRequest;
import com.taxflow.application.dto.TaxFlowDtos.FinancialHealthResponse;
import com.taxflow.application.dto.TaxFlowDtos.GstSummary;
import com.taxflow.application.dto.TaxFlowDtos.IncomeTaxRequest;
import com.taxflow.application.dto.TaxFlowDtos.IncomeTaxResponse;
import com.taxflow.application.dto.TaxFlowDtos.InsightResponse;
import com.taxflow.application.dto.TaxFlowDtos.KpiDashboardResponse;
import com.taxflow.application.dto.TaxFlowDtos.NotificationResponse;
import com.taxflow.application.dto.TaxFlowDtos.Recommendation;
import com.taxflow.application.dto.TaxFlowDtos.RuleRequest;
import com.taxflow.application.dto.TaxFlowDtos.RuleResponse;
import com.taxflow.common.exception.NotFoundException;
import com.taxflow.domain.enums.DocumentType;
import com.taxflow.domain.enums.ExpenseCategory;
import com.taxflow.domain.enums.FilingStatus;
import com.taxflow.domain.enums.InvoiceStatus;
import com.taxflow.domain.enums.InvoiceType;
import com.taxflow.domain.enums.RuleSeverity;
import com.taxflow.domain.enums.RuleType;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.model.Business;
import com.taxflow.domain.model.BusinessRule;
import com.taxflow.domain.model.Invoice;
import com.taxflow.domain.model.Product;
import com.taxflow.domain.model.TaxFiling;
import com.taxflow.infrastructure.repository.BusinessRuleRepository;
import com.taxflow.infrastructure.repository.CustomerRepository;
import com.taxflow.infrastructure.repository.DocumentRepository;
import com.taxflow.infrastructure.repository.ExpenseRepository;
import com.taxflow.infrastructure.repository.InvoiceRepository;
import com.taxflow.infrastructure.repository.NotificationRepository;
import com.taxflow.infrastructure.repository.ProductRepository;
import com.taxflow.infrastructure.repository.TaxFilingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxEngineService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BusinessService businessService;
    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final DocumentRepository documentRepository;
    private final NotificationRepository notificationRepository;
    private final TaxFilingRepository taxFilingRepository;
    private final BusinessRuleRepository ruleRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public GstSummary gstSummary(UUID businessId, LocalDate from, LocalDate to) {
        businessService.business(businessId, TaskScope.GST);
        LocalDate start = from == null ? YearMonth.now().atDay(1) : from;
        LocalDate end = to == null ? YearMonth.now().atEndOfMonth() : to;
        BigDecimal output = invoiceRepository.sumGst(businessId, InvoiceType.GST_INVOICE, start, end)
                .add(invoiceRepository.sumGst(businessId, InvoiceType.DEBIT_NOTE, start, end));
        BigDecimal input = invoiceRepository.sumGst(businessId, InvoiceType.PURCHASE_BILL, start, end)
                .add(expenseRepository.sumGst(businessId, start, end));
        BigDecimal net = output.subtract(input).max(BigDecimal.ZERO);
        BigDecimal lateFee = LocalDate.now().isAfter(end.plusDays(20)) ? BigDecimal.valueOf(50L * Math.max(1, LocalDate.now().toEpochDay() - end.plusDays(20).toEpochDay())) : BigDecimal.ZERO;
        BigDecimal interest = net.multiply(BigDecimal.valueOf(0.18)).divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(Math.max(0, LocalDate.now().toEpochDay() - end.plusDays(20).toEpochDay())));
        return new GstSummary(output, input, input, net, lateFee, interest,
                List.of("GSTR-1 due by 11th of next month", "GSTR-3B due by 20th of next month"));
    }

    public IncomeTaxResponse incomeTax(IncomeTaxRequest request) {
        BigDecimal gross = request.businessIncome().add(request.salaryIncome()).add(request.otherIncome());
        BigDecimal totalDeductions = request.investments().min(BigDecimal.valueOf(150000))
                .add(request.deductions())
                .add(request.businessExpenses());
        BigDecimal taxable = gross.subtract(totalDeductions).max(BigDecimal.ZERO);
        BigDecimal tax = slabTax(taxable);
        BigDecimal advance = tax.divide(BigDecimal.valueOf(4), 2, RoundingMode.HALF_UP);
        Map<String, BigDecimal> pnl = Map.of(
                "businessIncome", request.businessIncome(),
                "businessExpenses", request.businessExpenses(),
                "netProfit", request.businessIncome().subtract(request.businessExpenses()));
        List<String> suggestions = new ArrayList<>();
        if (request.investments().compareTo(BigDecimal.valueOf(150000)) < 0) {
            suggestions.add("Invest up to remaining 80C limit to reduce taxable income.");
        }
        if (request.businessExpenses().compareTo(request.businessIncome().multiply(BigDecimal.valueOf(0.15))) < 0) {
            suggestions.add("Review eligible business expenses such as rent, salary, software, maintenance, and travel.");
        }
        suggestions.add("Maintain bills and receipts for every claimed deduction.");
        return new IncomeTaxResponse(taxable, tax, advance, suggestions, pnl);
    }

    @Transactional(readOnly = true)
    public List<Recommendation> recommendations(UUID businessId, LocalDate from, LocalDate to) {
        businessService.business(businessId, TaskScope.INCOME_TAX);
        GstSummary gst = gstSummary(businessId, from, to);
        BigDecimal sales = invoiceRepository.sumTotal(businessId, InvoiceType.GST_INVOICE, from, to);
        BigDecimal expenses = expenseRepository.sumAmount(businessId, from, to);
        List<Recommendation> recommendations = new ArrayList<>();
        if (gst.inputGst().compareTo(BigDecimal.ZERO) == 0 && sales.compareTo(BigDecimal.ZERO) > 0) {
            recommendations.add(new Recommendation("ITC_MISSING", RuleSeverity.WARNING, "No ITC captured",
                    "You have sales but no input GST credit. Upload purchase bills and expense receipts before filing.", gst.outputGst().min(BigDecimal.valueOf(25000))));
        }
        if (expenses.compareTo(sales.multiply(BigDecimal.valueOf(0.1))) < 0) {
            recommendations.add(new Recommendation("LOW_EXPENSE_CAPTURE", RuleSeverity.INFO, "Expense capture looks low",
                    "Small businesses usually have rent, utilities, salaries, maintenance or travel expenses. Missing bills can increase taxable profit.", sales.multiply(BigDecimal.valueOf(0.03))));
        }
        recommendations.add(new Recommendation("ADVANCE_TAX", RuleSeverity.INFO, "Plan advance tax",
                "Set aside tax every month from profit so quarterly payments do not affect working capital.", gst.netGstPayable()));
        return recommendations;
    }

    @Transactional(readOnly = true)
    public List<ComplianceIssue> compliance(UUID businessId) {
        Business business = businessService.business(businessId, TaskScope.GST);
        List<ComplianceIssue> issues = new ArrayList<>();
        if (business.getGstin() == null || business.getGstin().isBlank()) {
            issues.add(new ComplianceIssue("MISSING_GSTIN", RuleSeverity.WARNING, "BUSINESS", "GSTIN is missing from the business profile."));
        }
        if (documentRepository.countByBusinessIdAndDocumentType(businessId, DocumentType.PAN) == 0) {
            issues.add(new ComplianceIssue("PAN_DOC_MISSING", RuleSeverity.WARNING, "DOCUMENT", "PAN document is missing."));
        }
        List<Invoice> invoices = invoiceRepository.findForPeriod(businessId, LocalDate.now().minusYears(1), LocalDate.now().plusDays(1));
        Map<String, Long> numbers = new LinkedHashMap<>();
        for (Invoice invoice : invoices) {
            numbers.put(invoice.getInvoiceNumber(), numbers.getOrDefault(invoice.getInvoiceNumber(), 0L) + 1);
            if (invoice.getLines().isEmpty() || invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                issues.add(new ComplianceIssue("INCOMPLETE_INVOICE", RuleSeverity.ERROR, "INVOICE", invoice.getInvoiceNumber() + " has no valid taxable lines."));
            }
            BigDecimal computed = invoice.getSubtotal().add(invoice.getTotalGst()).setScale(2, RoundingMode.HALF_UP);
            if (computed.compareTo(invoice.getTotalAmount().setScale(2, RoundingMode.HALF_UP)) != 0) {
                issues.add(new ComplianceIssue("TAX_MISMATCH", RuleSeverity.ERROR, "INVOICE", invoice.getInvoiceNumber() + " has inconsistent tax totals."));
            }
        }
        numbers.entrySet().stream().filter(e -> e.getValue() > 1)
                .forEach(e -> issues.add(new ComplianceIssue("DUPLICATE_INVOICE", RuleSeverity.ERROR, "INVOICE", "Duplicate invoice number " + e.getKey())));
        productRepository.findLowStock(businessId).stream().filter(p -> p.getStock().compareTo(BigDecimal.ZERO) < 0)
                .forEach(p -> issues.add(new ComplianceIssue("NEGATIVE_STOCK", RuleSeverity.ERROR, "PRODUCT", p.getSku() + " has negative stock.")));
        for (BusinessRule rule : ruleRepository.activeRules(businessId, RuleType.COMPLIANCE)) {
            evaluateRule(rule, businessId).ifPresent(issues::add);
        }
        return issues;
    }

    @Transactional(readOnly = true)
    public FinancialHealthResponse health(UUID businessId) {
        businessService.business(businessId, TaskScope.DASHBOARD);
        LocalDate start = YearMonth.now().atDay(1);
        LocalDate end = YearMonth.now().atEndOfMonth();
        BigDecimal sales = invoiceRepository.sumTotal(businessId, InvoiceType.GST_INVOICE, start, end);
        BigDecimal expenses = expenseRepository.sumAmount(businessId, start, end);
        BigDecimal profit = sales.subtract(expenses);
        BigDecimal overdue = invoiceRepository.overdueAmount(businessId, LocalDate.now());
        int pending = compliance(businessId).stream().mapToInt(issue -> issue.severity() == RuleSeverity.ERROR ? 10 : 4).sum();
        int score = 70;
        if (sales.compareTo(BigDecimal.ZERO) > 0 && profit.divide(sales, 4, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(0.18)) > 0) {
            score += 12;
        }
        if (overdue.compareTo(BigDecimal.ZERO) == 0) {
            score += 8;
        }
        score -= Math.min(35, pending);
        score = Math.max(0, Math.min(100, score));
        return new FinancialHealthResponse(score, score >= 80 ? "A" : score >= 65 ? "B" : score >= 50 ? "C" : "D",
                List.of("GST ledger is generated automatically", "Stock valuation is connected to invoices"),
                List.of("Collect overdue invoices faster", "Upload missing compliance documents", "Review low-stock products weekly"));
    }

    @Transactional(readOnly = true)
    public List<InsightResponse> insights(UUID businessId) {
        businessService.business(businessId, TaskScope.DASHBOARD);
        LocalDate start = YearMonth.now().atDay(1);
        LocalDate end = YearMonth.now().atEndOfMonth();
        BigDecimal sales = invoiceRepository.sumTotal(businessId, InvoiceType.GST_INVOICE, start, end);
        BigDecimal expenses = expenseRepository.sumAmount(businessId, start, end);
        String biggestExpense = expenseRepository.categoryBreakdown(businessId, start, end).stream()
                .max(Comparator.comparing(row -> (BigDecimal) row[1]))
                .map(row -> ((ExpenseCategory) row[0]).name())
                .orElse("NONE");
        Product low = productRepository.findLowStock(businessId).stream().findFirst().orElse(null);
        return List.of(
                new InsightResponse("Monthly profit", sales.subtract(expenses).toPlainString(), "Revenue minus recorded expenses for this month.", RuleSeverity.INFO),
                new InsightResponse("Biggest expense category", biggestExpense, "Use this to find cost-control opportunities.", RuleSeverity.INFO),
                new InsightResponse("Slow-moving inventory", low == null ? "None" : low.getName(), "Low or stale stock should be reordered carefully.", low == null ? RuleSeverity.INFO : RuleSeverity.WARNING)
        );
    }

    @Transactional(readOnly = true)
    public KpiDashboardResponse kpis(UUID businessId) {
        businessService.business(businessId, TaskScope.DASHBOARD);
        LocalDate start = YearMonth.now().atDay(1);
        LocalDate end = YearMonth.now().atEndOfMonth();
        BigDecimal sales = invoiceRepository.sumTotal(businessId, InvoiceType.GST_INVOICE, start, end);
        BigDecimal expenses = expenseRepository.sumAmount(businessId, start, end);
        BigDecimal inventory = productRepository.stockValue(businessId);
        BigDecimal profit = sales.subtract(expenses);
        BigDecimal grossMargin = ratio(profit, sales);
        BigDecimal overdue = invoiceRepository.overdueAmount(businessId, LocalDate.now());
        BigDecimal aov = ratio(sales, BigDecimal.valueOf(Math.max(1, invoiceRepository.countByBusinessIdAndType(businessId, InvoiceType.GST_INVOICE))));
        return new KpiDashboardResponse(grossMargin, grossMargin, ratio(sales, inventory.max(BigDecimal.ONE)),
                overdue.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(21),
                aov, BigDecimal.valueOf(42), BigDecimal.valueOf(8.4));
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UUID businessId) {
        businessService.business(businessId, TaskScope.DASHBOARD);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = YearMonth.now().atDay(1);
        LocalDate monthEnd = YearMonth.now().atEndOfMonth();
        BigDecimal todaySales = invoiceRepository.sumTotal(businessId, InvoiceType.GST_INVOICE, today, today);
        BigDecimal monthlySales = invoiceRepository.sumTotal(businessId, InvoiceType.GST_INVOICE, monthStart, monthEnd);
        BigDecimal expenses = expenseRepository.sumAmount(businessId, monthStart, monthEnd);
        GstSummary gst = gstSummary(businessId, monthStart, monthEnd);
        Map<String, BigDecimal> metrics = new LinkedHashMap<>();
        metrics.put("todaysSales", todaySales);
        metrics.put("monthlySales", monthlySales);
        metrics.put("monthlyGst", gst.netGstPayable());
        metrics.put("profit", monthlySales.subtract(expenses));
        metrics.put("expenses", expenses);
        metrics.put("taxDue", gst.netGstPayable());
        metrics.put("stockValue", productRepository.stockValue(businessId));
        metrics.put("cashFlow", monthlySales.subtract(expenses).subtract(gst.netGstPayable()));
        List<NotificationResponse> notifications = notificationRepository.findTop10ByBusinessIdAndUserIdOrderByCreatedAtDesc(
                businessId, businessService.get(businessId).getOwner().getId()).stream().map(notificationService::toResponse).toList();
        Map<String, List<BigDecimal>> charts = Map.of(
                "revenue", List.of(monthlySales.multiply(BigDecimal.valueOf(0.65)), monthlySales.multiply(BigDecimal.valueOf(0.8)), monthlySales),
                "gst", List.of(gst.inputGst(), gst.outputGst(), gst.netGstPayable()),
                "expenses", expenseRepository.categoryBreakdown(businessId, monthStart, monthEnd).stream().map(row -> (BigDecimal) row[1]).toList()
        );
        return new DashboardResponse(metrics,
                taxFilingRepository.countByBusinessIdAndStatusNot(businessId, FilingStatus.FILED),
                invoiceRepository.countByBusinessIdAndStatus(businessId, InvoiceStatus.SENT),
                productRepository.stockValue(businessId),
                metrics.get("cashFlow"),
                health(businessId).score(),
                insights(businessId),
                notifications,
                charts,
                kpis(businessId));
    }

    @Transactional
    public FilingResponse saveWizard(UUID businessId, FilingWizardRequest request) {
        Business business = businessService.business(businessId, request.filingType().name().startsWith("GST") || request.filingType().name().startsWith("GSTR")
                ? TaskScope.GST : TaskScope.INCOME_TAX);
        GstSummary gst = gstSummary(businessId, request.periodStart(), request.periodEnd());
        List<ComplianceIssue> issues = compliance(businessId);
        FilingStatus status = issues.stream().anyMatch(i -> i.severity() == RuleSeverity.ERROR)
                ? FilingStatus.BLOCKED
                : request.step() >= 5 ? FilingStatus.READY_FOR_REVIEW : FilingStatus.DRAFT;
        TaxFiling filing = taxFilingRepository.findByBusinessIdAndFilingTypeAndPeriodStartAndPeriodEnd(
                        businessId, request.filingType(), request.periodStart(), request.periodEnd())
                .orElse(TaxFiling.builder().business(business).filingType(request.filingType()).periodStart(request.periodStart()).periodEnd(request.periodEnd()).build());
        filing.setProgressPercent(Math.min(100, Math.max(0, request.step() * 20)));
        filing.setStatus(status);
        filing.setTaxDue(gst.netGstPayable());
        filing.setLateFee(gst.lateFee());
        try {
            filing.setSummaryJson(objectMapper.writeValueAsString(Map.of("answers", request.answers(), "gst", gst, "issues", issues)));
        } catch (Exception ex) {
            filing.setSummaryJson("{}");
        }
        taxFilingRepository.save(filing);
        auditService.log(businessId, "SAVE_WIZARD", "TAX_FILING", filing.getId(), null, status.name());
        return toFilingResponse(filing);
    }

    @Transactional(readOnly = true)
    public com.taxflow.common.PageResponse<FilingResponse> filings(UUID businessId, org.springframework.data.domain.Pageable pageable) {
        businessService.business(businessId, TaskScope.GST);
        return com.taxflow.common.PageResponse.from(taxFilingRepository.findByBusinessId(businessId, pageable), this::toFilingResponse);
    }

    @Transactional
    public FilingResponse submit(UUID businessId, UUID filingId) {
        businessService.business(businessId, TaskScope.GST);
        TaxFiling filing = taxFilingRepository.findById(filingId).orElseThrow(() -> new NotFoundException("Filing not found"));
        if (!filing.getBusiness().getId().equals(businessId)) {
            throw new NotFoundException("Filing not found");
        }
        filing.setStatus(FilingStatus.SUBMITTED);
        filing.setProgressPercent(100);
        filing.setSubmittedAt(java.time.OffsetDateTime.now());
        auditService.log(businessId, "SUBMIT", "TAX_FILING", filingId, null, filing.getStatus().name());
        return toFilingResponse(filing);
    }

    @Transactional
    public RuleResponse createRule(RuleRequest request) {
        Business business = request.businessId() == null ? null : businessService.business(request.businessId(), TaskScope.SETTINGS);
        BusinessRule rule = ruleRepository.save(BusinessRule.builder()
                .business(business)
                .code(request.code())
                .name(request.name())
                .ruleType(request.ruleType())
                .severity(request.severity())
                .enabled(request.enabled())
                .expressionKey(request.expressionKey())
                .message(request.message())
                .build());
        return toRuleResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> rules() {
        return ruleRepository.findAll().stream().map(this::toRuleResponse).toList();
    }

    private java.util.Optional<ComplianceIssue> evaluateRule(BusinessRule rule, UUID businessId) {
        if ("LOW_ITC_CAPTURE".equals(rule.getExpressionKey())) {
            GstSummary gst = gstSummary(businessId, YearMonth.now().atDay(1), YearMonth.now().atEndOfMonth());
            if (gst.outputGst().compareTo(BigDecimal.ZERO) > 0 && gst.inputGst().compareTo(BigDecimal.ZERO) == 0) {
                return java.util.Optional.of(new ComplianceIssue(rule.getCode(), rule.getSeverity(), "GST", rule.getMessage()));
            }
        }
        return java.util.Optional.empty();
    }

    private BigDecimal slabTax(BigDecimal taxable) {
        if (taxable.compareTo(BigDecimal.valueOf(250000)) <= 0) return BigDecimal.ZERO;
        if (taxable.compareTo(BigDecimal.valueOf(500000)) <= 0) return taxable.subtract(BigDecimal.valueOf(250000)).multiply(BigDecimal.valueOf(0.05));
        if (taxable.compareTo(BigDecimal.valueOf(1000000)) <= 0) return BigDecimal.valueOf(12500).add(taxable.subtract(BigDecimal.valueOf(500000)).multiply(BigDecimal.valueOf(0.20)));
        return BigDecimal.valueOf(112500).add(taxable.subtract(BigDecimal.valueOf(1000000)).multiply(BigDecimal.valueOf(0.30)));
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(HUNDRED).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private FilingResponse toFilingResponse(TaxFiling filing) {
        return new FilingResponse(filing.getId(), filing.getFilingType(), filing.getStatus(), filing.getPeriodStart(),
                filing.getPeriodEnd(), filing.getProgressPercent(), filing.getTaxDue(), filing.getLateFee(),
                filing.getSummaryJson(), filing.getSubmittedAt());
    }

    private RuleResponse toRuleResponse(BusinessRule rule) {
        return new RuleResponse(rule.getId(), rule.getBusiness() == null ? null : rule.getBusiness().getId(), rule.getCode(),
                rule.getName(), rule.getRuleType(), rule.getSeverity(), rule.isEnabled(), rule.getExpressionKey(), rule.getMessage());
    }
}
