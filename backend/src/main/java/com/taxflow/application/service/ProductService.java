package com.taxflow.application.service;

import com.taxflow.application.dto.TaxFlowDtos.InventoryMovementRequest;
import com.taxflow.application.dto.TaxFlowDtos.InventoryMovementResponse;
import com.taxflow.application.dto.TaxFlowDtos.ProductRequest;
import com.taxflow.application.dto.TaxFlowDtos.ProductResponse;
import com.taxflow.application.mapper.ProductMapper;
import com.taxflow.common.InputSanitizer;
import com.taxflow.common.PageResponse;
import com.taxflow.common.exception.BusinessException;
import com.taxflow.common.exception.NotFoundException;
import com.taxflow.domain.enums.InventoryMovementType;
import com.taxflow.domain.enums.NotificationType;
import com.taxflow.domain.enums.TaskScope;
import com.taxflow.domain.model.Business;
import com.taxflow.domain.model.InventoryMovement;
import com.taxflow.domain.model.Notification;
import com.taxflow.domain.model.Product;
import com.taxflow.infrastructure.repository.InventoryMovementRepository;
import com.taxflow.infrastructure.repository.NotificationRepository;
import com.taxflow.infrastructure.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;
    private final NotificationRepository notificationRepository;
    private final BusinessService businessService;
    private final ProductMapper mapper;
    private final InputSanitizer sanitizer;
    private final AccessService accessService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(UUID businessId, String query, String category, Pageable pageable) {
        businessService.business(businessId, TaskScope.PRODUCT);
        Specification<Product> spec = (root, cq, cb) -> cb.equal(root.get("business").get("id"), businessId);
        if (query != null && !query.isBlank()) {
            String like = "%" + query.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("sku")), like)));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and((root, cq, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        }
        return PageResponse.from(productRepository.findAll(spec, pageable), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> lowStock(UUID businessId) {
        businessService.business(businessId, TaskScope.INVENTORY);
        return productRepository.findLowStock(businessId).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public ProductResponse create(UUID businessId, ProductRequest request) {
        Business business = businessService.business(businessId, TaskScope.PRODUCT);
        productRepository.findByBusinessIdAndSkuIgnoreCase(businessId, request.sku())
                .ifPresent(p -> {
                    throw new BusinessException("SKU already exists");
                });
        Product product = apply(Product.builder().business(business).build(), request);
        productRepository.save(product);
        auditService.log(businessId, "CREATE", "PRODUCT", product.getId(), null, product.getSku());
        return mapper.toResponse(product);
    }

    @Transactional
    public ProductResponse update(UUID businessId, UUID productId, ProductRequest request) {
        businessService.business(businessId, TaskScope.PRODUCT);
        Product product = require(businessId, productId);
        String old = product.getSku();
        apply(product, request);
        auditService.log(businessId, "UPDATE", "PRODUCT", productId, old, product.getSku());
        return mapper.toResponse(product);
    }

    @Transactional
    public void delete(UUID businessId, UUID productId) {
        businessService.business(businessId, TaskScope.PRODUCT);
        Product product = require(businessId, productId);
        productRepository.delete(product);
        auditService.log(businessId, "DELETE", "PRODUCT", productId, product.getSku(), null);
    }

    @Transactional
    public InventoryMovementResponse move(UUID businessId, InventoryMovementRequest request) {
        Business business = businessService.business(businessId, TaskScope.INVENTORY);
        Product product = require(businessId, request.productId());
        BigDecimal signed = switch (request.type()) {
            case STOCK_IN, PURCHASE, ADJUSTMENT -> request.quantity();
            case STOCK_OUT, SALE, EXPIRED -> request.quantity().negate();
        };
        changeStock(product, signed);
        InventoryMovement movement = movementRepository.save(InventoryMovement.builder()
                .business(business)
                .product(product)
                .type(request.type())
                .quantity(request.quantity())
                .unitCost(request.unitCost())
                .reason(sanitizer.clean(request.reason()))
                .referenceNumber(sanitizer.clean(request.referenceNumber()))
                .build());
        if (product.getStock().compareTo(product.getLowStockThreshold()) <= 0) {
            notificationRepository.save(Notification.builder()
                    .business(business)
                    .user(accessService.currentUser())
                    .type(NotificationType.LOW_STOCK)
                    .title("Low stock: " + product.getName())
                    .message("Current stock is " + product.getStock() + ", below threshold " + product.getLowStockThreshold())
                    .readFlag(false)
                    .actionUrl("/inventory")
                    .dueDate(LocalDate.now())
                    .build());
        }
        auditService.log(businessId, "STOCK_MOVE", "PRODUCT", product.getId(), null, request.type().name());
        return toMovementResponse(movement);
    }

    public Product require(UUID businessId, UUID productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new NotFoundException("Product not found"));
        if (!product.getBusiness().getId().equals(businessId)) {
            throw new NotFoundException("Product not found");
        }
        return product;
    }

    public void changeStock(Product product, BigDecimal delta) {
        BigDecimal next = product.getStock().add(delta);
        if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Negative stock is not allowed for " + product.getName());
        }
        product.setStock(next);
    }

    private Product apply(Product product, ProductRequest request) {
        product.setName(sanitizer.clean(request.name()));
        product.setCategory(sanitizer.clean(request.category()));
        product.setSku(sanitizer.clean(request.sku()));
        product.setHsnCode(sanitizer.clean(request.hsnCode()));
        product.setGstPercentage(request.gstPercentage());
        product.setStock(request.stock());
        product.setLowStockThreshold(request.lowStockThreshold());
        product.setPurchasePrice(request.purchasePrice());
        product.setSellingPrice(request.sellingPrice());
        product.setBarcode(sanitizer.clean(request.barcode()));
        product.setExpiryDate(request.expiryDate());
        return product;
    }

    private InventoryMovementResponse toMovementResponse(InventoryMovement movement) {
        return new InventoryMovementResponse(movement.getId(), movement.getProduct().getId(), movement.getProduct().getName(),
                movement.getType(), movement.getQuantity(), movement.getUnitCost(), movement.getReason(),
                movement.getReferenceNumber(), movement.getCreatedAt());
    }
}
