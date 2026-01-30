package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductVariantPositionRequest;
import com.ecommerse.backend.dto.ProductVariantRequest;
import com.ecommerse.backend.dto.ProductVariantResponse;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.entities.ProductVariant;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.ProductVariantRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates business rules for managing product variants.
 */
@Service
@Transactional
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public ProductVariantService(ProductRepository productRepository,
            ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> listVariants(Long productId) {
        ensureProductExists(productId);
        return productVariantRepository.findByProductIdOrderByPositionAsc(productId).stream()
                .map(ProductVariantResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse getVariant(Long productId, Long variantId) {
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Variant not found for product: " + productId + " and variant: " + variantId));
        return new ProductVariantResponse(variant);
    }

    public ProductVariantResponse createVariant(Long productId, @Valid ProductVariantRequest request) {
        Product product = getManagedProduct(productId);

        ensureUniqueSku(request.getSku(), null);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        applyRequestToVariant(variant, request);
        variant.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        variant.setActive(request.getActive() != null ? request.getActive() : Boolean.TRUE);
        variant.setDefaultVariant(Boolean.TRUE.equals(request.getDefaultVariant()));

        int nextPosition = calculateNextPosition(productId);
        Integer requestedPosition = request.getPosition();
        variant.setPosition(requestedPosition != null && requestedPosition >= 0 ? requestedPosition : nextPosition);

        normalizeVariantOptions(variant, request.getOptions());

        ProductVariant saved = productVariantRepository.save(variant);

        handleDefaultVariantAfterCreate(productId, saved, request.getDefaultVariant());
        rebalancePositions(productId);
        recalculateProductStock(product);

        return new ProductVariantResponse(saved);
    }

    public ProductVariantResponse updateVariant(Long productId, Long variantId, @Valid ProductVariantRequest request) {
        Product product = getManagedProduct(productId);

        ProductVariant existing = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Variant not found for product: " + productId + " and variant: " + variantId));

        if (!Objects.equals(existing.getSku(), request.getSku())) {
            ensureUniqueSku(request.getSku(), variantId);
        }

        boolean wasDefault = Boolean.TRUE.equals(existing.getDefaultVariant());

        applyRequestToVariant(existing, request);
        normalizeVariantOptions(existing, request.getOptions());

        if (request.getPosition() != null && request.getPosition() >= 0) {
            existing.setPosition(request.getPosition());
        }

        existing.setDefaultVariant(Boolean.TRUE.equals(request.getDefaultVariant()));
        existing.setActive(request.getActive() != null ? request.getActive() : Boolean.TRUE);
        existing.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);

        ProductVariant saved = productVariantRepository.save(existing);

        if (Boolean.TRUE.equals(saved.getDefaultVariant())) {
            productVariantRepository.clearDefaultVariantExcept(productId, saved.getId());
        } else if (wasDefault) {
            ensureDefaultVariantExists(productId);
        }

        rebalancePositions(productId);
        recalculateProductStock(product);

        return new ProductVariantResponse(saved);

    }

    public void deleteVariant(Long productId, Long variantId) {
        Product product = getManagedProduct(productId);

        ProductVariant existing = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Variant not found for product: " + productId + " and variant: " + variantId));

        boolean wasDefault = Boolean.TRUE.equals(existing.getDefaultVariant());

        productVariantRepository.delete(existing);
        productVariantRepository.flush();

        if (wasDefault) {
            ensureDefaultVariantExists(productId);
        }

        rebalancePositions(productId);
        recalculateProductStock(product);
    }

    public ProductVariantResponse setDefaultVariant(Long productId, Long variantId) {
        ProductVariant variant = productVariantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Variant not found for product: " + productId + " and variant: " + variantId));

        productVariantRepository.clearAllDefaultFlags(productId);
        variant.setDefaultVariant(Boolean.TRUE);
        ProductVariant saved = productVariantRepository.save(variant);

        return new ProductVariantResponse(saved);
    }

    public List<ProductVariantResponse> reorderVariants(Long productId,
            List<@Valid ProductVariantPositionRequest> positions) {

        if (CollectionUtils.isEmpty(positions)) {
            throw new IllegalArgumentException("At least one variant position is required");
        }

        ensureProductExists(productId);

        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByPositionAsc(productId);
        if (variants.size() != positions.stream().map(ProductVariantPositionRequest::getVariantId).distinct().count()) {
            throw new IllegalArgumentException("All variants must be included exactly once for reorder");
        }

        Map<Long, Integer> positionsById = new LinkedHashMap<>();
        for (ProductVariantPositionRequest request : positions) {
            positionsById.put(request.getVariantId(), request.getPosition());
        }

        Set<Long> variantIds = variants.stream().map(ProductVariant::getId).collect(Collectors.toSet());
        if (!variantIds.equals(positionsById.keySet())) {
            throw new IllegalArgumentException("Variant identifiers mismatch for product: " + productId);
        }

        variants.forEach(variant -> {
            Integer requested = positionsById.get(variant.getId());
            variant.setPosition(requested != null && requested >= 0 ? requested : variant.getPosition());
        });

        rebalancePositions(productId);

        return productVariantRepository.findByProductIdOrderByPositionAsc(productId).stream()
                .map(ProductVariantResponse::new)
                .collect(Collectors.toList());
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Product not found with id: " + productId);
        }
    }

    private Product getManagedProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
    }

    private void ensureUniqueSku(String sku, Long excludeId) {
        if (excludeId == null) {
            if (productVariantRepository.existsBySku(sku)) {
                throw new IllegalArgumentException("Variant with SKU '" + sku + "' already exists");
            }
        } else if (productVariantRepository.existsBySkuAndIdNot(sku, excludeId)) {
            throw new IllegalArgumentException("Variant with SKU '" + sku + "' already exists");
        }
    }

    private void applyRequestToVariant(ProductVariant variant, ProductVariantRequest request) {
        variant.setName(request.getName());
        variant.setSku(request.getSku());
        variant.setPrice(request.getPrice());
        variant.setImageUrl(request.getImageUrl());
    }

    private int calculateNextPosition(Long productId) {
        return productVariantRepository.findTopByProductIdOrderByPositionDesc(productId)
                .map(existing -> existing.getPosition() != null ? existing.getPosition() + 1 : 0)
                .orElse(0);
    }

    private void normalizeVariantOptions(ProductVariant variant, Map<String, String> rawOptions) {
        if (rawOptions == null || rawOptions.isEmpty()) {
            variant.setOptions(Map.of());
            return;
        }

        Map<String, String> sanitized = rawOptions.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().trim().isEmpty())
                .collect(Collectors.toMap(
                        entry -> entry.getKey().trim(),
                        entry -> entry.getValue() != null ? entry.getValue().trim() : "",
                        (left, right) -> right,
                        LinkedHashMap::new));

        variant.setOptions(sanitized);
    }

    private void handleDefaultVariantAfterCreate(Long productId, ProductVariant saved, Boolean requestedDefault) {
        if (Boolean.TRUE.equals(requestedDefault)) {
            productVariantRepository.clearDefaultVariantExcept(productId, saved.getId());
            saved.setDefaultVariant(Boolean.TRUE);
        } else if (productVariantRepository.countByProductIdAndDefaultVariantTrue(productId) == 0) {
            saved.setDefaultVariant(Boolean.TRUE);
        }
        productVariantRepository.save(saved);
    }

    private void rebalancePositions(Long productId) {
        List<ProductVariant> variants = new ArrayList<>(
                productVariantRepository.findByProductIdOrderByPositionAsc(productId));
        variants.sort(Comparator.comparing(variant -> variant.getPosition() != null ? variant.getPosition() : 0));

        int index = 0;
        for (ProductVariant variant : variants) {
            variant.setPosition(index++);
        }

        productVariantRepository.saveAll(variants);
    }

    private void ensureDefaultVariantExists(Long productId) {
        if (productVariantRepository.countByProductIdAndDefaultVariantTrue(productId) > 0) {
            return;
        }

        productVariantRepository.findByProductIdOrderByPositionAsc(productId).stream().findFirst()
                .ifPresent(variant -> {
                    variant.setDefaultVariant(Boolean.TRUE);
                    productVariantRepository.save(variant);
                });
    }

    private void recalculateProductStock(Product product) {
        Integer summedStock = productVariantRepository.sumActiveStockByProductId(product.getId());
        int total = summedStock != null ? summedStock : 0;
        product.setStockQuantity(total);
        productRepository.save(product);
    }
}
