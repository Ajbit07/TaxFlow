package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.ComplianceIssue;
import com.taxflow.application.dto.TaxFlowDtos.FilingResponse;
import com.taxflow.application.dto.TaxFlowDtos.FilingWizardRequest;
import com.taxflow.application.dto.TaxFlowDtos.FinancialHealthResponse;
import com.taxflow.application.dto.TaxFlowDtos.GstSummary;
import com.taxflow.application.dto.TaxFlowDtos.IncomeTaxRequest;
import com.taxflow.application.dto.TaxFlowDtos.IncomeTaxResponse;
import com.taxflow.application.dto.TaxFlowDtos.InsightResponse;
import com.taxflow.application.dto.TaxFlowDtos.KpiDashboardResponse;
import com.taxflow.application.dto.TaxFlowDtos.Recommendation;
import com.taxflow.application.service.TaxEngineService;
import com.taxflow.common.ApiResponse;
import com.taxflow.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/tax")
@RequiredArgsConstructor
public class TaxController {
    private final TaxEngineService taxEngineService;

    @GetMapping("/gst-summary")
    public ApiResponse<GstSummary> gstSummary(@PathVariable UUID businessId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(taxEngineService.gstSummary(businessId, from, to));
    }

    @PostMapping("/income-tax")
    public ApiResponse<IncomeTaxResponse> incomeTax(@PathVariable UUID businessId,
                                                    @Valid @RequestBody IncomeTaxRequest request) {
        return ApiResponse.ok(taxEngineService.incomeTax(request));
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<Recommendation>> recommendations(@PathVariable UUID businessId,
                                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(taxEngineService.recommendations(businessId, from, to));
    }

    @GetMapping("/compliance")
    public ApiResponse<List<ComplianceIssue>> compliance(@PathVariable UUID businessId) {
        return ApiResponse.ok(taxEngineService.compliance(businessId));
    }

    @GetMapping("/health")
    public ApiResponse<FinancialHealthResponse> health(@PathVariable UUID businessId) {
        return ApiResponse.ok(taxEngineService.health(businessId));
    }

    @GetMapping("/insights")
    public ApiResponse<List<InsightResponse>> insights(@PathVariable UUID businessId) {
        return ApiResponse.ok(taxEngineService.insights(businessId));
    }

    @GetMapping("/kpis")
    public ApiResponse<KpiDashboardResponse> kpis(@PathVariable UUID businessId) {
        return ApiResponse.ok(taxEngineService.kpis(businessId));
    }

    @GetMapping("/filings")
    public ApiResponse<PageResponse<FilingResponse>> filings(@PathVariable UUID businessId,
                                                             @PageableDefault(size = 20, sort = "periodEnd", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(taxEngineService.filings(businessId, pageable));
    }

    @PostMapping("/filings/wizard")
    public ApiResponse<FilingResponse> saveWizard(@PathVariable UUID businessId,
                                                  @Valid @RequestBody FilingWizardRequest request) {
        return ApiResponse.message("Filing progress saved", taxEngineService.saveWizard(businessId, request));
    }

    @PostMapping("/filings/{filingId}/submit")
    public ApiResponse<FilingResponse> submit(@PathVariable UUID businessId, @PathVariable UUID filingId) {
        return ApiResponse.message("Filing submitted", taxEngineService.submit(businessId, filingId));
    }
}
