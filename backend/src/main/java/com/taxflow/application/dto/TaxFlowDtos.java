package com.taxflow.application.dto;

import com.taxflow.domain.enums.BusinessType;
import com.taxflow.domain.enums.DocumentType;
import com.taxflow.domain.enums.ExpenseCategory;
import com.taxflow.domain.enums.FilingStatus;
import com.taxflow.domain.enums.FilingType;
import com.taxflow.domain.enums.InventoryMovementType;
import com.taxflow.domain.enums.InvoiceStatus;
import com.taxflow.domain.enums.InvoiceType;
import com.taxflow.domain.enums.NotificationType;
import com.taxflow.domain.enums.RuleSeverity;
import com.taxflow.domain.enums.RuleType;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.enums.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TaxFlowDtos {
    private TaxFlowDtos() {
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @Size(min = 8, max = 100) String password,
            @NotBlank String fullName,
            String phone,
            UserRole role
    ) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record AuthResponse(UUID userId, String email, String fullName, UserRole role, String accessToken, String refreshToken, boolean emailVerified) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record ForgotPasswordRequest(@Email @NotBlank String email) {
    }

    public record ResetPasswordRequest(@NotBlank String token, @Size(min = 8, max = 100) String newPassword) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @Size(min = 8, max = 100) String newPassword) {
    }

    public record BusinessRequest(
            @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$", message = "Invalid GSTIN") String gstin,
            @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "Invalid PAN") @NotBlank String pan,
            @NotBlank String businessName,
            @NotBlank String ownerName,
            @NotBlank String address,
            @NotBlank String phone,
            @Email @NotBlank String email,
            @NotBlank String state,
            @NotNull BusinessType businessType,
            @NotBlank String financialYear,
            String currency,
            String language,
            boolean darkMode
    ) {
    }

    public record BusinessResponse(UUID id, UUID ownerId, String gstin, String pan, String businessName, String ownerName,
                                   String address, String phone, String email, String state, BusinessType businessType,
                                   String financialYear, String currency, String language, boolean darkMode) {
    }

    public record AssignEmployeeRequest(@Email @NotBlank String email, @NotEmpty Set<TaskScope> scopes) {
    }

    public record CustomerRequest(@NotBlank String name, String gstin, String pan, String phone, String email, String address,
                                  BigDecimal openingBalance) {
    }

    public record CustomerResponse(UUID id, String name, String gstin, String pan, String phone, String email, String address,
                                   BigDecimal outstandingBalance, BigDecimal lifetimeSales, long invoiceCount) {
    }

    public record ProductRequest(@NotBlank String name, @NotBlank String category, @NotBlank String sku,
                                 @NotBlank String hsnCode, @NotNull @DecimalMin("0.0") BigDecimal gstPercentage,
                                 @NotNull @DecimalMin("0.0") BigDecimal stock,
                                 @NotNull @DecimalMin("0.0") BigDecimal lowStockThreshold,
                                 @NotNull @DecimalMin("0.0") BigDecimal purchasePrice,
                                 @NotNull @DecimalMin("0.0") BigDecimal sellingPrice,
                                 String barcode, LocalDate expiryDate) {
    }

    public record ProductResponse(UUID id, String name, String category, String sku, String hsnCode, BigDecimal gstPercentage,
                                  BigDecimal stock, BigDecimal lowStockThreshold, BigDecimal purchasePrice,
                                  BigDecimal sellingPrice, String barcode, LocalDate expiryDate,
                                  boolean lowStock, BigDecimal inventoryValue, BigDecimal grossMargin) {
    }

    public record InventoryMovementRequest(@NotNull UUID productId, @NotNull InventoryMovementType type,
                                           @NotNull @DecimalMin("0.001") BigDecimal quantity,
                                           @NotNull @DecimalMin("0.0") BigDecimal unitCost,
                                           @NotBlank String reason, String referenceNumber) {
    }

    public record InventoryMovementResponse(UUID id, UUID productId, String productName, InventoryMovementType type,
                                            BigDecimal quantity, BigDecimal unitCost, String reason,
                                            String referenceNumber, OffsetDateTime createdAt) {
    }

    public record InvoiceLineRequest(UUID productId, @NotBlank String description, @NotBlank String hsnCode,
                                     @NotNull @DecimalMin("0.001") BigDecimal quantity,
                                     @NotNull @DecimalMin("0.0") BigDecimal unitPrice,
                                     @NotNull @DecimalMin("0.0") BigDecimal gstRate) {
    }

    public record InvoiceLineResponse(UUID id, UUID productId, String description, String hsnCode, BigDecimal quantity,
                                      BigDecimal unitPrice, BigDecimal gstRate, BigDecimal taxableAmount,
                                      BigDecimal gstAmount, BigDecimal totalAmount) {
    }

    public record InvoiceRequest(UUID customerId, String invoiceNumber, @NotNull InvoiceType type,
                                 @NotNull InvoiceStatus status, @NotNull LocalDate invoiceDate,
                                 @FutureOrPresent LocalDate dueDate, boolean recurring, String recurrencePattern,
                                 String templateName, BigDecimal paidAmount, String notes,
                                 @NotEmpty List<@Valid InvoiceLineRequest> lines) {
    }

    public record InvoiceResponse(UUID id, UUID businessId, UUID customerId, String customerName, String invoiceNumber,
                                  InvoiceType type, InvoiceStatus status, LocalDate invoiceDate, LocalDate dueDate,
                                  boolean recurring, String recurrencePattern, String templateName, BigDecimal subtotal,
                                  BigDecimal totalGst, BigDecimal totalAmount, BigDecimal paidAmount, String notes,
                                  String qrPayload, List<InvoiceLineResponse> lines, OffsetDateTime createdAt) {
    }

    public record ExpenseRequest(@NotNull ExpenseCategory category, @NotBlank String vendor,
                                 @NotNull @DecimalMin("0.0") BigDecimal amount,
                                 @NotNull @DecimalMin("0.0") BigDecimal gstAmount,
                                 @NotNull LocalDate expenseDate, String receiptUrl, String description) {
    }

    public record ExpenseResponse(UUID id, ExpenseCategory category, String vendor, BigDecimal amount, BigDecimal gstAmount,
                                  LocalDate expenseDate, String receiptUrl, String description) {
    }

    public record DocumentResponse(UUID id, DocumentType documentType, String fileName, String contentType, String storagePath,
                                   long fileSize, int version, String status, String extractedFieldsJson,
                                   OffsetDateTime createdAt) {
    }

    public record FilingWizardRequest(@NotNull FilingType filingType, @NotNull LocalDate periodStart,
                                      @NotNull LocalDate periodEnd, int step, Map<String, Object> answers) {
    }

    public record FilingResponse(UUID id, FilingType filingType, FilingStatus status, LocalDate periodStart,
                                 LocalDate periodEnd, int progressPercent, BigDecimal taxDue, BigDecimal lateFee,
                                 String summaryJson, OffsetDateTime submittedAt) {
    }

    public record GstSummary(BigDecimal outputGst, BigDecimal inputGst, BigDecimal itcAvailable,
                             BigDecimal netGstPayable, BigDecimal lateFee, BigDecimal interest,
                             List<String> reminders) {
    }

    public record IncomeTaxRequest(@NotNull @DecimalMin("0.0") BigDecimal businessIncome,
                                   @NotNull @DecimalMin("0.0") BigDecimal salaryIncome,
                                   @NotNull @DecimalMin("0.0") BigDecimal otherIncome,
                                   @NotNull @DecimalMin("0.0") BigDecimal investments,
                                   @NotNull @DecimalMin("0.0") BigDecimal deductions,
                                   @NotNull @DecimalMin("0.0") BigDecimal businessExpenses) {
    }

    public record IncomeTaxResponse(BigDecimal taxableIncome, BigDecimal estimatedTax, BigDecimal advanceTaxDue,
                                    List<String> taxSavingSuggestions, Map<String, BigDecimal> profitAndLoss) {
    }

    public record Recommendation(String code, RuleSeverity severity, String title, String message, BigDecimal estimatedImpact) {
    }

    public record ComplianceIssue(String code, RuleSeverity severity, String entity, String message) {
    }

    public record FinancialHealthResponse(int score, String grade, List<String> strengths, List<String> recommendations) {
    }

    public record InsightResponse(String title, String value, String explanation, RuleSeverity severity) {
    }

    public record KpiDashboardResponse(BigDecimal grossMargin, BigDecimal netProfitMargin, BigDecimal inventoryTurnover,
                                       BigDecimal receivableDays, BigDecimal averageOrderValue,
                                       BigDecimal repeatCustomerRate, BigDecimal monthlyGrowth) {
    }

    public record DashboardResponse(Map<String, BigDecimal> metrics, long pendingFilings, long pendingInvoices,
                                    BigDecimal stockValue, BigDecimal cashFlow, int healthScore,
                                    List<InsightResponse> insights, List<NotificationResponse> notifications,
                                    Map<String, List<BigDecimal>> charts, KpiDashboardResponse kpis) {
    }

    public record NotificationResponse(UUID id, NotificationType type, String title, String message, boolean readFlag,
                                       String actionUrl, LocalDate dueDate, OffsetDateTime createdAt) {
    }

    public record RuleRequest(UUID businessId, @NotBlank String code, @NotBlank String name, @NotNull RuleType ruleType,
                              @NotNull RuleSeverity severity, boolean enabled, @NotBlank String expressionKey,
                              @NotBlank String message) {
    }

    public record RuleResponse(UUID id, UUID businessId, String code, String name, RuleType ruleType,
                               RuleSeverity severity, boolean enabled, String expressionKey, String message) {
    }

    public record SearchResult(String type, UUID id, String title, String subtitle, String url) {
    }

    public record ReportResponse(String name, String contentType, byte[] bytes) {
    }

    public record AuditResponse(Long id, UUID userId, UUID businessId, OffsetDateTime actionTime, String ipAddress,
                                String action, String entityType, String entityId, String oldValue,
                                String newValue, boolean success, String errorMessage) {
    }

    public record AdminUserResponse(UUID id, String email, String fullName, UserRole role, boolean enabled,
                                    boolean emailVerified, OffsetDateTime createdAt) {
    }
}
