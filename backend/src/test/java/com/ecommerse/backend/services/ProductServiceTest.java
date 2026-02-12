package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductDTO;
import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.repositories.CategoryRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductService using Mockito
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private Category testCategory;
    private ProductDTO testProductDTO;

    @BeforeEach
    void setUp() {
        // Setup test category
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Electronics");
        testCategory.setDescription("Electronic devices");
        testCategory.setActive(true);

        // Setup test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("iPhone 15 Pro");
        testProduct.setDescription("Latest iPhone with advanced camera");
        testProduct.setPrice(new BigDecimal("999.99"));
        testProduct.setStockQuantity(50);
        testProduct.setSku("IPH15P-256-BLK");
        testProduct.setActive(true);
        testProduct.setFeatured(false);
        testProduct.setCategory(testCategory);
        testProduct.setCreatedDate(LocalDateTime.now());
        testProduct.setUpdatedDate(LocalDateTime.now());

        // Setup test DTO
        testProductDTO = new ProductDTO();
        testProductDTO.setId(1L);
        testProductDTO.setName("iPhone 15 Pro");
        testProductDTO.setDescription("Latest iPhone with advanced camera");
        testProductDTO.setPrice(new BigDecimal("999.99"));
        testProductDTO.setStockQuantity(50);
        testProductDTO.setSku("IPH15P-256-BLK");
        testProductDTO.setActive(true);
        testProductDTO.setFeatured(false);
        testProductDTO.setCategoryId(1L);
    }

    @Test
    void getAllProducts_ShouldReturnPageOfProducts() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, pageable, 1);

        when(productRepository.findAllByActiveTrue(pageable))
                .thenReturn(productPage);

        // When
        Page<ProductDTO> result = productService.getAllProducts(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("iPhone 15 Pro");
        verify(productRepository).findAllByActiveTrue(pageable);
    }

    @Test
    void getCatalogProducts_WithCategoryFilter_ShouldUseFilteredRepository() {
        Pageable pageable = PageRequest.of(0, 12);
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products, pageable, 1);

        when(productRepository.findActiveForCatalog(2L, null, pageable)).thenReturn(productPage);

        Page<ProductDTO> result = productService.getCatalogProducts(pageable, 2L, null);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategoryId()).isEqualTo(testProductDTO.getCategoryId());
        verify(productRepository).findActiveForCatalog(2L, null, pageable);
    }

    @Test
    void getProductById_WhenProductExists_ShouldReturnProductDTO() {
        // Given
        when(productRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(testProduct));

        // When
        Optional<ProductDTO> result = productService.getProductById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("iPhone 15 Pro");
        assertThat(result.get().getSku()).isEqualTo("IPH15P-256-BLK");
        verify(productRepository).findByIdAndActiveTrue(1L);
    }

    @Test
    void getProductById_WhenProductNotExists_ShouldReturnEmpty() {
        // Given
        when(productRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        // When
        Optional<ProductDTO> result = productService.getProductById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(productRepository).findByIdAndActiveTrue(999L);
    }

    @Test
    void createProduct_WithValidData_ShouldCreateProduct() {
        // Given
        when(productRepository.existsBySku(testProductDTO.getSku())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        // Return the saved entity so assertions can observe service-side mutations (e.g., generated SKU).
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductDTO result = productService.createProduct(testProductDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(result.getCategoryId()).isEqualTo(1L);
        assertThat(result.getSku()).isEqualTo("IPH15P-256-BLK");
        verify(productRepository).existsBySku(testProductDTO.getSku());
        verify(categoryRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_WithBlankSku_ShouldAutoGenerateSku() {
        // Given
        testProductDTO.setSku("");
        when(productRepository.existsBySku(anyString())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ProductDTO result = productService.createProduct(testProductDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isNotBlank();
        assertThat(result.getSku()).hasSizeLessThanOrEqualTo(50);
        assertThat(result.getSku()).contains("-");
        verify(productRepository, atLeastOnce()).existsBySku(anyString());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_WithDuplicateSku_ShouldThrowException() {
        // Given
        when(productRepository.existsBySku(testProductDTO.getSku())).thenReturn(true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(testProductDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(productRepository).existsBySku(testProductDTO.getSku());
        verify(categoryRepository).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_WithInvalidCategory_ShouldThrowException() {
        // Given
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(testProductDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");

        verify(categoryRepository).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_WithZeroPrice_ShouldThrowException() {
        // Given
        testProductDTO.setPrice(BigDecimal.ZERO);

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(testProductDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("price must be greater than 0");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_WithValidData_ShouldUpdateProduct() {
        // Given
        ProductDTO updateDTO = new ProductDTO();
        updateDTO.setName("iPhone 15 Pro Max");
        updateDTO.setDescription("Updated description");
        updateDTO.setPrice(new BigDecimal("1199.99"));
        updateDTO.setStockQuantity(30);
        updateDTO.setSku("IPH15P-256-BLK"); // Same SKU
        updateDTO.setCategoryId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        ProductDTO result = productService.updateProduct(1L, updateDTO);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).findById(1L);
        verify(categoryRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_WithNullSku_ShouldNotClearOrRegenerateSku() {
        // Given
        ProductDTO updateDTO = new ProductDTO();
        updateDTO.setName("iPhone 15 Pro Max");
        updateDTO.setDescription("Updated description");
        updateDTO.setPrice(new BigDecimal("1199.99"));
        updateDTO.setStockQuantity(30);
        updateDTO.setSku(null); // omitted
        updateDTO.setCategoryId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        ProductDTO result = productService.updateProduct(1L, updateDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(testProduct.getSku()).isEqualTo("IPH15P-256-BLK");
        verify(productRepository, never()).existsBySku(anyString());
    }

    @Test
    void updateProduct_WithNonExistentProduct_ShouldThrowException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(999L, testProductDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_WithExistingProduct_ShouldSoftDelete() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
        // Verify that the product's active status would be set to false
        // (We can't directly verify this without capturing the argument)
    }

    @Test
    void deleteProduct_WithNonExistentProduct_ShouldThrowException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getFeaturedProducts_ShouldReturnFeaturedProducts() {
        // Given
        testProduct.setFeatured(true);
        List<Product> featuredProducts = Arrays.asList(testProduct);
        when(productRepository.findByFeaturedTrueAndActiveTrueOrderByCreatedDateDesc())
                .thenReturn(featuredProducts);

        // When
        List<ProductDTO> result = productService.getFeaturedProducts();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFeatured()).isTrue();
        verify(productRepository).findByFeaturedTrueAndActiveTrueOrderByCreatedDateDesc();
    }

    @Test
    void searchProducts_WithValidTerm_ShouldReturnMatchingProducts() {
        // Given
        String searchTerm = "iPhone";
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products, pageable, 1);

        // ProductService wraps search terms to a LIKE pattern: %term%
        when(productRepository.findActiveForCatalog(null, "%iphone%", pageable))
                .thenReturn(productPage);

        // When
        Page<ProductDTO> result = productService.searchProducts(searchTerm, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("iPhone");
        verify(productRepository).findActiveForCatalog(null, "%iphone%", pageable);
    }

    @Test
    void updateStock_WithValidQuantity_ShouldUpdateStock() {
        // Given
        Integer newQuantity = 100;
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        productService.updateStock(1L, newQuantity);

        // Then
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getLowStockProducts_ShouldReturnProductsBelowThreshold() {
        // Given
        Integer threshold = 10;
        testProduct.setStockQuantity(5); // Below threshold
        List<Product> lowStockProducts = Arrays.asList(testProduct);
        when(productRepository.findByActiveTrueAndStockQuantityLessThanOrderByStockQuantityAsc(threshold))
                .thenReturn(lowStockProducts);

        // When
        List<ProductDTO> result = productService.getLowStockProducts(threshold);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockQuantity()).isLessThan(threshold);
        verify(productRepository).findByActiveTrueAndStockQuantityLessThanOrderByStockQuantityAsc(threshold);
    }
}
