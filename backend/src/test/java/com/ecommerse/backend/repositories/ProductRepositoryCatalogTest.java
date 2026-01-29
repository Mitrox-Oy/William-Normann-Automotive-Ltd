package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.entities.ProductImage;
import com.ecommerse.backend.entities.ProductVariant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryCatalogTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findActiveForCatalog_ShouldReturnProductWithImagesAndVariants() {
        Category category = new Category();
        category.setName("Smartphones");
        category.setSlug("smartphones");
        category.setDescription("Phones and accessories");
        category.setActive(true);
        category.setSortOrder(1);
        Category savedCategory = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setName("iPhone 15 Pro");
        product.setDescription("Flagship device with titanium frame");
        product.setPrice(new BigDecimal("1199.00"));
        product.setStockQuantity(10);
        product.setSku("IPH15P-256-OBSIDIAN");
        product.setActive(true);
        product.setFeatured(true);
        product.setBrand("Apple");
        product.setCategory(savedCategory);

        ProductImage image = new ProductImage();
        image.setImageUrl("uploads/products/iphone15-main.jpg");
        image.setPosition(0);
        image.setIsMain(true);
        image.setProduct(product);
        product.getImages().add(image);

        ProductVariant variant = new ProductVariant(product, "Obsidian · 256 GB", "IPH15P-256-OBS");
        variant.setPrice(new BigDecimal("1249.00"));
        variant.setStockQuantity(5);
        variant.setDefaultVariant(true);
        variant.setPosition(0);
        variant.setOptions(Map.of("color", "Obsidian", "storage", "256 GB"));
        product.addVariant(variant);

        productRepository.saveAndFlush(product);

        Pageable pageable = PageRequest.of(0, 12);
        Page<Product> page = productRepository.findActiveForCatalog(savedCategory.getId(), "iphone", pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        Product result = page.getContent().get(0);

        assertThat(result.getImages()).hasSize(1);
        assertThat(result.getImages().get(0).getImageUrl()).endsWith("iphone15-main.jpg");
        assertThat(result.getVariants()).hasSize(1);
        ProductVariant resultVariant = result.getVariants().iterator().next();
        assertThat(resultVariant.getSku()).isEqualTo("IPH15P-256-OBS");
        assertThat(resultVariant.getOptions()).containsEntry("color", "Obsidian");
    }
}
