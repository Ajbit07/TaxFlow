package com.taxflow.application.mapper;

import com.taxflow.application.dto.TaxFlowDtos.ProductResponse;
import com.taxflow.domain.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getSku(),
                product.getHsnCode(),
                product.getGstPercentage(),
                product.getStock(),
                product.getLowStockThreshold(),
                product.getPurchasePrice(),
                product.getSellingPrice(),
                product.getBarcode(),
                product.getExpiryDate(),
                product.getStock().compareTo(product.getLowStockThreshold()) <= 0,
                product.getStock().multiply(product.getPurchasePrice()),
                product.getSellingPrice().subtract(product.getPurchasePrice()));
    }
}
