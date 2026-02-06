package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.repositories.CategoryRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LoadTestDataService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public LoadTestDataService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public record CreateProductsResult(
            String runId,
            int countRequested,
            int countCreated,
            String skuPrefix,
            List<String> sampleSkus
    ) {
    }

    public record DeleteProductsResult(
            String runId,
            boolean hardDeleteRequested,
            int matched,
            int hardDeleted,
            int softDeleted,
            int failed
    ) {
    }

    @Transactional
    public CreateProductsResult createProducts(Integer count, Integer variantsPerProductIgnored) {
        int resolvedCount = count == null ? 300 : Math.max(1, Math.min(count, 5000));
        String runId = generateRunId();

        List<Category> activeCategories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        if (activeCategories.isEmpty()) {
            throw new IllegalStateException("No active categories found. Create categories first.");
        }

        // Prefer leaf categories to spread products across existing structure.
        Set<Long> parentIds = new HashSet<>();
        for (Category c : activeCategories) {
            Category parent = c.getParent();
            if (parent != null && parent.getId() != null) {
                parentIds.add(parent.getId());
            }
        }

        List<Category> candidates = new ArrayList<>();
        for (Category c : activeCategories) {
            if (c.getId() != null && !parentIds.contains(c.getId())) {
                candidates.add(c);
            }
        }
        if (candidates.isEmpty()) {
            candidates = activeCategories;
        }

        String skuPrefix = "LT-" + runId + "-";

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<Product> toCreate = new ArrayList<>(resolvedCount);

        // Rotate through categories so distribution is stable (not clumped by RNG).
        int categoryCount = candidates.size();
        int startOffset = rnd.nextInt(categoryCount);

        for (int i = 1; i <= resolvedCount; i++) {
            Category category = candidates.get((startOffset + i) % categoryCount);

            Product p = new Product();
            p.setCategory(category);

            String sku = skuPrefix + String.format(Locale.ROOT, "%04d", i);
            p.setSku(sku);
            p.setName("Load Test Product " + i);
            p.setDescription("[LOADTEST runId=" + runId + "] Generated at " + OffsetDateTime.now());

            BigDecimal price = BigDecimal.valueOf(rnd.nextDouble(5.0, 5000.0))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            p.setPrice(price);

            p.setStockQuantity(rnd.nextInt(0, 250));
            p.setActive(Boolean.TRUE);
            p.setFeatured(Boolean.FALSE);
            p.setQuoteOnly(Boolean.FALSE);
            p.setBrand("LoadTest");
            p.setProductType(resolveProductType(category));

            toCreate.add(p);
        }

        productRepository.saveAll(toCreate);

        List<String> sampleSkus = toCreate.stream()
                .limit(10)
                .map(Product::getSku)
                .toList();

        return new CreateProductsResult(runId, resolvedCount, resolvedCount, skuPrefix, sampleSkus);
    }

    @Transactional
    public DeleteProductsResult deleteProducts(String runId, boolean hardDelete) {
        if (runId == null || runId.trim().isEmpty()) {
            throw new IllegalArgumentException("runId is required");
        }

        String prefix = "LT-" + runId.trim() + "-";
        List<Product> matched = productRepository.findBySkuStartingWith(prefix);

        int hardDeleted = 0;
        int softDeleted = 0;
        int failed = 0;

        if (!hardDelete) {
            for (Product p : matched) {
                p.setActive(Boolean.FALSE);
            }
            try {
                productRepository.saveAll(matched);
                softDeleted = matched.size();
            } catch (Exception ex) {
                // Fall back to per-row update so we can report partial failures.
                for (Product p : matched) {
                    try {
                        p.setActive(Boolean.FALSE);
                        productRepository.save(p);
                        softDeleted++;
                    } catch (Exception inner) {
                        failed++;
                    }
                }
            }

            return new DeleteProductsResult(runId, false, matched.size(), 0, softDeleted, failed);
        }

        for (Product p : matched) {
            try {
                productRepository.delete(p);
                hardDeleted++;
            } catch (Exception ex) {
                // Fall back to soft delete if hard delete fails due to constraints.
                try {
                    p.setActive(Boolean.FALSE);
                    productRepository.save(p);
                    softDeleted++;
                } catch (Exception inner) {
                    failed++;
                }
            }
        }

        return new DeleteProductsResult(runId, hardDelete, matched.size(), hardDeleted, softDeleted, failed);
    }

    private String generateRunId() {
        long a = System.currentTimeMillis();
        long b = ThreadLocalRandom.current().nextLong();
        String raw = Long.toString(a, 36) + Long.toString(Math.abs(b), 36);
        return raw.length() > 12 ? raw.substring(0, 12) : raw;
    }

    private String resolveProductType(Category category) {
        if (category == null) {
            return null;
        }

        Category current = category;
        while (current.getParent() != null) {
            current = current.getParent();
        }

        String slug = current.getSlug();
        if (slug == null) {
            return null;
        }

        return switch (slug.toLowerCase(Locale.ROOT)) {
            case "cars" -> "car";
            case "parts" -> "part";
            case "tools" -> "tool";
            case "custom" -> "custom";
            default -> null;
        };
    }
}
