package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.DiscountCodeDTO;
import com.ecommerse.backend.dto.DiscountPreviewDTO;
import com.ecommerse.backend.dto.DiscountPreviewRequest;
import com.ecommerse.backend.entities.CartItem;
import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.entities.DiscountCode;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.repositories.CategoryRepository;
import com.ecommerse.backend.repositories.DiscountCodeRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscountService {

    public record AppliedDiscount(
            boolean applied,
            String code,
            BigDecimal percentage,
            Set<Long> eligibleProductIds) {

        public static AppliedDiscount none() {
            return new AppliedDiscount(false, null, BigDecimal.ZERO, Collections.emptySet());
        }

        public boolean appliesToProduct(Long productId) {
            return productId != null && eligibleProductIds.contains(productId);
        }
    }

    private final DiscountCodeRepository discountCodeRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PricingService pricingService;

    public DiscountService(DiscountCodeRepository discountCodeRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            PricingService pricingService) {
        this.discountCodeRepository = discountCodeRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public List<DiscountCodeDTO> getAllDiscountCodes() {
        return discountCodeRepository.findAllByOrderByCreatedDateDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public DiscountCodeDTO createDiscountCode(DiscountCodeDTO dto) {
        String normalizedCode = normalizeCode(dto.getCode());
        if (discountCodeRepository.findByCodeIgnoreCase(normalizedCode).isPresent()) {
            throw new IllegalArgumentException("Discount code already exists: " + normalizedCode);
        }

        DiscountCode discountCode = new DiscountCode();
        applyDtoToEntity(discountCode, dto, normalizedCode);
        return toDto(discountCodeRepository.save(discountCode));
    }

    @Transactional
    public DiscountCodeDTO updateDiscountCode(Long id, DiscountCodeDTO dto) {
        DiscountCode existing = discountCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Discount code not found: " + id));

        String normalizedCode = normalizeCode(dto.getCode());
        discountCodeRepository.findByCodeIgnoreCase(normalizedCode)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Discount code already exists: " + normalizedCode);
                });

        applyDtoToEntity(existing, dto, normalizedCode);
        return toDto(discountCodeRepository.save(existing));
    }

    @Transactional
    public void deleteDiscountCode(Long id) {
        if (!discountCodeRepository.existsById(id)) {
            throw new IllegalArgumentException("Discount code not found: " + id);
        }
        discountCodeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public DiscountPreviewDTO previewDiscount(DiscountPreviewRequest request) {
        String normalizedCode = normalizeCode(request.getCode());

        DiscountCode discountCode = discountCodeRepository.findByCodeIgnoreCaseAndActiveTrue(normalizedCode)
                .orElse(null);

        List<DiscountPreviewRequest.PreviewItem> requestedItems = Optional.ofNullable(request.getItems())
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(item -> item != null && item.getProductId() != null && item.getQuantity() != null
                        && item.getQuantity() > 0)
                .toList();

        Set<Long> productIds = requestedItems.stream().map(DiscountPreviewRequest.PreviewItem::getProductId)
                .collect(Collectors.toSet());
        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .filter(Product::getActive)
                .collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal saleSavings = BigDecimal.ZERO;
        BigDecimal codeSavings = BigDecimal.ZERO;
        Set<Long> eligibleProductIds = new HashSet<>();

        if (discountCode != null) {
            for (DiscountPreviewRequest.PreviewItem line : requestedItems) {
                Product product = productsById.get(line.getProductId());
                if (product != null && isProductEligible(discountCode, product)) {
                    eligibleProductIds.add(product.getId());
                }
            }
        }

        for (DiscountPreviewRequest.PreviewItem line : requestedItems) {
            Product product = productsById.get(line.getProductId());
            if (product == null) {
                continue;
            }

            BigDecimal unitPrice = pricingService.resolveEffectiveUnitPrice(product);
            BigDecimal quantity = BigDecimal.valueOf(line.getQuantity());

            subtotal = subtotal.add(unitPrice.multiply(quantity));
            saleSavings = saleSavings.add(
                    pricingService.resolveSaleSavingsPerUnit(product).multiply(quantity));

            if (discountCode != null && eligibleProductIds.contains(product.getId())) {
                BigDecimal discountedUnit = pricingService.applyPercentageDiscount(unitPrice, discountCode.getPercentage());
                BigDecimal lineCodeSavings = unitPrice.subtract(discountedUnit).multiply(quantity);
                codeSavings = codeSavings.add(lineCodeSavings);
            }
        }

        subtotal = pricingService.roundCurrency(subtotal);
        saleSavings = pricingService.roundCurrency(saleSavings);
        codeSavings = pricingService.roundCurrency(codeSavings);

        DiscountPreviewDTO response = new DiscountPreviewDTO();
        response.setCode(normalizedCode);
        response.setSubtotal(subtotal);
        response.setSaleSavings(saleSavings);
        response.setCodeSavings(codeSavings);
        response.setTotalSavings(pricingService.roundCurrency(saleSavings.add(codeSavings)));
        response.setTotalAfterDiscount(pricingService.roundCurrency(subtotal.subtract(codeSavings)));

        if (discountCode == null) {
            response.setValid(false);
            response.setMessage("Discount code is invalid or inactive");
            response.setPercentage(BigDecimal.ZERO);
            return response;
        }

        if (eligibleProductIds.isEmpty()) {
            response.setValid(false);
            response.setMessage("Discount code does not apply to products in your cart");
            response.setPercentage(discountCode.getPercentage());
            return response;
        }

        response.setValid(true);
        response.setMessage("Discount code applied");
        response.setCode(discountCode.getCode());
        response.setPercentage(discountCode.getPercentage());
        return response;
    }

    @Transactional(readOnly = true)
    public AppliedDiscount resolveAppliedDiscount(String rawCode, List<CartItem> cartItems) {
        String normalizedCode = normalizeCode(rawCode);
        if (normalizedCode == null) {
            return AppliedDiscount.none();
        }

        DiscountCode discountCode = discountCodeRepository.findByCodeIgnoreCaseAndActiveTrue(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Discount code is invalid or inactive"));

        Set<Long> eligibleProductIds = cartItems.stream()
                .map(CartItem::getProduct)
                .filter(Objects::nonNull)
                .filter(product -> isProductEligible(discountCode, product))
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (eligibleProductIds.isEmpty()) {
            throw new IllegalArgumentException("Discount code does not apply to products in cart");
        }

        return new AppliedDiscount(true, discountCode.getCode(), discountCode.getPercentage(), eligibleProductIds);
    }

    private void applyDtoToEntity(DiscountCode entity, DiscountCodeDTO dto, String normalizedCode) {
        if (dto.getPercentage() == null || dto.getPercentage().compareTo(BigDecimal.ZERO) <= 0
                || dto.getPercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage must be greater than 0 and at most 100");
        }

        entity.setCode(normalizedCode);
        entity.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        entity.setPercentage(pricingService.roundCurrency(dto.getPercentage()));
        entity.setActive(dto.getActive() == null || dto.getActive());

        boolean appliesToAll = dto.getAppliesToAllProducts() == null || dto.getAppliesToAllProducts();
        entity.setAppliesToAllProducts(appliesToAll);

        if (appliesToAll) {
            entity.getCategories().clear();
            return;
        }

        List<Long> categoryIds = Optional.ofNullable(dto.getCategoryIds()).orElseGet(Collections::emptyList).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one category or enable applies-to-all");
        }

        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new IllegalArgumentException("One or more selected categories do not exist");
        }

        entity.getCategories().clear();
        entity.getCategories().addAll(categories);
    }

    private DiscountCodeDTO toDto(DiscountCode entity) {
        DiscountCodeDTO dto = new DiscountCodeDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setDescription(entity.getDescription());
        dto.setPercentage(entity.getPercentage());
        dto.setActive(entity.getActive());
        dto.setAppliesToAllProducts(entity.getAppliesToAllProducts());
        dto.setCategoryIds(entity.getCategories().stream().map(Category::getId).toList());
        dto.setCategoryNames(entity.getCategories().stream().map(Category::getName).toList());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        return dto;
    }

    private String normalizeCode(String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String normalized = rawCode.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isProductEligible(DiscountCode discountCode, Product product) {
        if (discountCode == null || product == null) {
            return false;
        }
        if (Boolean.TRUE.equals(discountCode.getAppliesToAllProducts())) {
            return true;
        }
        if (discountCode.getCategories() == null || discountCode.getCategories().isEmpty()) {
            return false;
        }

        Set<Long> eligibleCategoryIds = discountCode.getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        Category cursor = product.getCategory();
        while (cursor != null) {
            if (eligibleCategoryIds.contains(cursor.getId())) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }
}
