package com.taxflow.web;

import com.taxflow.application.dto.TaxFlowDtos.InventoryMovementRequest;
import com.taxflow.application.dto.TaxFlowDtos.InventoryMovementResponse;
import com.taxflow.application.dto.TaxFlowDtos.ProductRequest;
import com.taxflow.application.dto.TaxFlowDtos.ProductResponse;
import com.taxflow.application.service.ProductService;
import com.taxflow.common.ApiResponse;
import com.taxflow.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> list(@PathVariable UUID businessId,
                                                           @RequestParam(required = false) String query,
                                                           @RequestParam(required = false) String category,
                                                           @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.ok(productService.list(businessId, query, category, pageable));
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<ProductResponse>> lowStock(@PathVariable UUID businessId) {
        return ApiResponse.ok(productService.lowStock(businessId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@PathVariable UUID businessId, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.message("Product created", productService.create(businessId, request));
    }

    @PutMapping("/{productId}")
    public ApiResponse<ProductResponse> update(@PathVariable UUID businessId, @PathVariable UUID productId,
                                               @Valid @RequestBody ProductRequest request) {
        return ApiResponse.message("Product updated", productService.update(businessId, productId, request));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> delete(@PathVariable UUID businessId, @PathVariable UUID productId) {
        productService.delete(businessId, productId);
        return ApiResponse.message("Product deleted", null);
    }

    @PostMapping("/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryMovementResponse> move(@PathVariable UUID businessId,
                                                       @Valid @RequestBody InventoryMovementRequest request) {
        return ApiResponse.message("Stock movement recorded", productService.move(businessId, request));
    }
}
