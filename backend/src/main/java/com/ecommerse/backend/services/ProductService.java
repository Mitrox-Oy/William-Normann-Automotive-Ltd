package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductDTO;
import com.ecommerse.backend.dto.ProductImageResponse;
import com.ecommerse.backend.dto.ProductVariantResponse;
import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.entities.ProductVariant;
import com.ecommerse.backend.repositories.CategoryRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class for managing products
 */
@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Get all active products with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return getCatalogProducts(pageable, null, null);
    }

    /**
     * Catalog listing with optional category and keyword filters.
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> getCatalogProducts(Pageable pageable, Long categoryId, String searchTerm) {
        String normalizedSearch = (searchTerm != null && !searchTerm.trim().isEmpty())
                ? searchTerm.trim().toLowerCase()
                : null;

        Page<Product> page;
        if (categoryId != null || normalizedSearch != null) {
            page = productRepository.findActiveForCatalog(categoryId, normalizedSearch, pageable);
        } else {
            page = productRepository.findAllByActiveTrue(pageable);
        }
        return page.map(this::convertToDTO);
    }

    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public Optional<ProductDTO> getProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .map(this::convertToDTO);
    }

    /**
     * Get product by SKU (case-insensitive)
     */
    @Transactional(readOnly = true)
    public Optional<ProductDTO> getProductBySku(String sku) {
        return productRepository.findBySkuIgnoreCaseAndActiveTrue(sku)
                .map(this::convertToDTO);
    }

    /**
     * Get products by category
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        return getCatalogProducts(pageable, categoryId, null);
    }

    /**
     * Get featured products
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getFeaturedProducts() {
        return productRepository.findByFeaturedTrueAndActiveTrueOrderByCreatedDateDesc()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Search products
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String searchTerm, Pageable pageable) {
        return getCatalogProducts(pageable, null, searchTerm);
    }

    /**
     * Get products by price range
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByActiveTrueAndPriceBetweenOrderByPriceAsc(minPrice, maxPrice, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Create new product (Owner only)
     */
    public ProductDTO createProduct(ProductDTO productDTO) {
        validateProductData(productDTO);

        if (productRepository.existsBySku(productDTO.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + productDTO.getSku() + "' already exists");
        }

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found with id: " + productDTO.getCategoryId()));

        Product product = convertToEntity(productDTO);
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }

    /**
     * Update product (Owner only)
     */
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        validateProductData(productDTO);

        // Check SKU uniqueness if changed
        if (!existingProduct.getSku().equals(productDTO.getSku()) &&
                productRepository.existsBySku(productDTO.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + productDTO.getSku() + "' already exists");
        }

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found with id: " + productDTO.getCategoryId()));

        updateProductEntity(existingProduct, productDTO);
        existingProduct.setCategory(category);

        Product savedProduct = productRepository.save(existingProduct);
        return convertToDTO(savedProduct);
    }

    /**
     * Delete product (Owner only) - soft delete
     */
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * Update stock quantity
     */
    public void updateStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        product.setStockQuantity(quantity);
        productRepository.save(product);
    }

    /**
     * Get low stock products
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getLowStockProducts(Integer threshold) {
        return productRepository.findByActiveTrueAndStockQuantityLessThanOrderByStockQuantityAsc(threshold)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    private void validateProductData(ProductDTO productDTO) {
        if (productDTO.getPrice() == null || productDTO.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price must be at least 0");
        }

        if (productDTO.getStockQuantity() != null && productDTO.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
    }

    private Product convertToEntity(ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
        product.setSku(dto.getSku());
        product.setImageUrl(dto.getImageUrl());
        product.setActive(dto.getActive() != null ? dto.getActive() : true);
        product.setFeatured(dto.getFeatured() != null ? dto.getFeatured() : false);
        product.setWeight(dto.getWeight());
        product.setBrand(dto.getBrand());
        product.setInfoSection1Title(dto.getInfoSection1Title());
        product.setInfoSection1Content(dto.getInfoSection1Content());
        product.setInfoSection1Enabled(dto.getInfoSection1Enabled() != null ? dto.getInfoSection1Enabled() : Boolean.FALSE);
        product.setInfoSection2Title(dto.getInfoSection2Title());
        product.setInfoSection2Content(dto.getInfoSection2Content());
        product.setInfoSection2Enabled(dto.getInfoSection2Enabled() != null ? dto.getInfoSection2Enabled() : Boolean.FALSE);
        product.setInfoSection3Title(dto.getInfoSection3Title());
        product.setInfoSection3Content(dto.getInfoSection3Content());
        product.setInfoSection3Enabled(dto.getInfoSection3Enabled() != null ? dto.getInfoSection3Enabled() : Boolean.FALSE);
        product.setInfoSection4Title(dto.getInfoSection4Title());
        product.setInfoSection4Content(dto.getInfoSection4Content());
        product.setInfoSection4Enabled(dto.getInfoSection4Enabled() != null ? dto.getInfoSection4Enabled() : Boolean.FALSE);
        product.setInfoSection5Title(dto.getInfoSection5Title());
        product.setInfoSection5Content(dto.getInfoSection5Content());
        product.setInfoSection5Enabled(dto.getInfoSection5Enabled() != null ? dto.getInfoSection5Enabled() : Boolean.FALSE);
        product.setInfoSection6Title(dto.getInfoSection6Title());
        product.setInfoSection6Content(dto.getInfoSection6Content());
        product.setInfoSection6Enabled(dto.getInfoSection6Enabled() != null ? dto.getInfoSection6Enabled() : Boolean.FALSE);
        product.setInfoSection7Title(dto.getInfoSection7Title());
        product.setInfoSection7Content(dto.getInfoSection7Content());
        product.setInfoSection7Enabled(dto.getInfoSection7Enabled() != null ? dto.getInfoSection7Enabled() : Boolean.FALSE);
        product.setInfoSection8Title(dto.getInfoSection8Title());
        product.setInfoSection8Content(dto.getInfoSection8Content());
        product.setInfoSection8Enabled(dto.getInfoSection8Enabled() != null ? dto.getInfoSection8Enabled() : Boolean.FALSE);
        product.setInfoSection9Title(dto.getInfoSection9Title());
        product.setInfoSection9Content(dto.getInfoSection9Content());
        product.setInfoSection9Enabled(dto.getInfoSection9Enabled() != null ? dto.getInfoSection9Enabled() : Boolean.FALSE);
        product.setInfoSection10Title(dto.getInfoSection10Title());
        product.setInfoSection10Content(dto.getInfoSection10Content());
        product.setInfoSection10Enabled(dto.getInfoSection10Enabled() != null ? dto.getInfoSection10Enabled() : Boolean.FALSE);
        return product;
    }

    private void updateProductEntity(Product product, ProductDTO dto) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
        product.setSku(dto.getSku());
        product.setImageUrl(dto.getImageUrl());
        product.setActive(dto.getActive() != null ? dto.getActive() : true);
        product.setFeatured(dto.getFeatured() != null ? dto.getFeatured() : false);
        product.setWeight(dto.getWeight());
        product.setBrand(dto.getBrand());
        product.setInfoSection1Title(dto.getInfoSection1Title());
        product.setInfoSection1Content(dto.getInfoSection1Content());
        product.setInfoSection1Enabled(dto.getInfoSection1Enabled() != null ? dto.getInfoSection1Enabled() : Boolean.FALSE);
        product.setInfoSection2Title(dto.getInfoSection2Title());
        product.setInfoSection2Content(dto.getInfoSection2Content());
        product.setInfoSection2Enabled(dto.getInfoSection2Enabled() != null ? dto.getInfoSection2Enabled() : Boolean.FALSE);
        product.setInfoSection3Title(dto.getInfoSection3Title());
        product.setInfoSection3Content(dto.getInfoSection3Content());
        product.setInfoSection3Enabled(dto.getInfoSection3Enabled() != null ? dto.getInfoSection3Enabled() : Boolean.FALSE);
        product.setInfoSection4Title(dto.getInfoSection4Title());
        product.setInfoSection4Content(dto.getInfoSection4Content());
        product.setInfoSection4Enabled(dto.getInfoSection4Enabled() != null ? dto.getInfoSection4Enabled() : Boolean.FALSE);
        product.setInfoSection5Title(dto.getInfoSection5Title());
        product.setInfoSection5Content(dto.getInfoSection5Content());
        product.setInfoSection5Enabled(dto.getInfoSection5Enabled() != null ? dto.getInfoSection5Enabled() : Boolean.FALSE);
        product.setInfoSection6Title(dto.getInfoSection6Title());
        product.setInfoSection6Content(dto.getInfoSection6Content());
        product.setInfoSection6Enabled(dto.getInfoSection6Enabled() != null ? dto.getInfoSection6Enabled() : Boolean.FALSE);
        product.setInfoSection7Title(dto.getInfoSection7Title());
        product.setInfoSection7Content(dto.getInfoSection7Content());
        product.setInfoSection7Enabled(dto.getInfoSection7Enabled() != null ? dto.getInfoSection7Enabled() : Boolean.FALSE);
        product.setInfoSection8Title(dto.getInfoSection8Title());
        product.setInfoSection8Content(dto.getInfoSection8Content());
        product.setInfoSection8Enabled(dto.getInfoSection8Enabled() != null ? dto.getInfoSection8Enabled() : Boolean.FALSE);
        product.setInfoSection9Title(dto.getInfoSection9Title());
        product.setInfoSection9Content(dto.getInfoSection9Content());
        product.setInfoSection9Enabled(dto.getInfoSection9Enabled() != null ? dto.getInfoSection9Enabled() : Boolean.FALSE);
        product.setInfoSection10Title(dto.getInfoSection10Title());
        product.setInfoSection10Content(dto.getInfoSection10Content());
        product.setInfoSection10Enabled(dto.getInfoSection10Enabled() != null ? dto.getInfoSection10Enabled() : Boolean.FALSE);
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setSku(product.getSku());
        dto.setImageUrl(product.getImageUrl());
        dto.setActive(product.getActive());
        dto.setFeatured(product.getFeatured());
        dto.setWeight(product.getWeight());
        dto.setBrand(product.getBrand());
        dto.setInfoSection1Title(product.getInfoSection1Title());
        dto.setInfoSection1Content(product.getInfoSection1Content());
        dto.setInfoSection1Enabled(product.getInfoSection1Enabled());
        dto.setInfoSection2Title(product.getInfoSection2Title());
        dto.setInfoSection2Content(product.getInfoSection2Content());
        dto.setInfoSection2Enabled(product.getInfoSection2Enabled());
        dto.setInfoSection3Title(product.getInfoSection3Title());
        dto.setInfoSection3Content(product.getInfoSection3Content());
        dto.setInfoSection3Enabled(product.getInfoSection3Enabled());
        dto.setInfoSection4Title(product.getInfoSection4Title());
        dto.setInfoSection4Content(product.getInfoSection4Content());
        dto.setInfoSection4Enabled(product.getInfoSection4Enabled());
        dto.setInfoSection5Title(product.getInfoSection5Title());
        dto.setInfoSection5Content(product.getInfoSection5Content());
        dto.setInfoSection5Enabled(product.getInfoSection5Enabled());
        dto.setInfoSection6Title(product.getInfoSection6Title());
        dto.setInfoSection6Content(product.getInfoSection6Content());
        dto.setInfoSection6Enabled(product.getInfoSection6Enabled());
        dto.setInfoSection7Title(product.getInfoSection7Title());
        dto.setInfoSection7Content(product.getInfoSection7Content());
        dto.setInfoSection7Enabled(product.getInfoSection7Enabled());
        dto.setInfoSection8Title(product.getInfoSection8Title());
        dto.setInfoSection8Content(product.getInfoSection8Content());
        dto.setInfoSection8Enabled(product.getInfoSection8Enabled());
        dto.setInfoSection9Title(product.getInfoSection9Title());
        dto.setInfoSection9Content(product.getInfoSection9Content());
        dto.setInfoSection9Enabled(product.getInfoSection9Enabled());
        dto.setInfoSection10Title(product.getInfoSection10Title());
        dto.setInfoSection10Content(product.getInfoSection10Content());
        dto.setInfoSection10Enabled(product.getInfoSection10Enabled());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryName(product.getCategory().getName());
        dto.setInStock(product.isInStock());
        dto.setAvailable(product.isAvailable());
        dto.setCreatedDate(product.getCreatedDate());
        dto.setUpdatedDate(product.getUpdatedDate());

        // Convert ProductImage entities to ProductImageResponse DTOs
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            java.util.List<ProductImageResponse> imageResponses = product.getImages().stream()
                    .map(ProductImageResponse::new)
                    .sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
                    .collect(java.util.stream.Collectors.toList());
            dto.setImages(imageResponses);
        }

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            java.util.List<ProductVariantResponse> variantResponses = product.getVariants().stream()
                    .filter(variant -> variant.getActive() == null || Boolean.TRUE.equals(variant.getActive()))
                    .sorted((a, b) -> {
                        int aPos = a.getPosition() != null ? a.getPosition() : Integer.MAX_VALUE;
                        int bPos = b.getPosition() != null ? b.getPosition() : Integer.MAX_VALUE;
                        return Integer.compare(aPos, bPos);
                    })
                    .map(ProductVariantResponse::new)
                    .collect(java.util.stream.Collectors.toList());
            dto.setVariants(variantResponses);
        }

        return dto;
    }

    /**
     * Advanced search with multiple filters
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> advancedSearch(String query, Long categoryId, BigDecimal minPrice,
            BigDecimal maxPrice, String brand, Boolean inStockOnly,
            Boolean featuredOnly, Pageable pageable) {
        return productRepository.findWithFilters(query, categoryId, minPrice, maxPrice,
                brand, inStockOnly, featuredOnly, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Bulk update products
     */
    public List<ProductDTO> bulkUpdateProducts(List<ProductDTO> productDTOs) {
        List<ProductDTO> updatedProducts = new ArrayList<>();

        for (ProductDTO dto : productDTOs) {
            if (dto.getId() == null) {
                throw new IllegalArgumentException("Product ID is required for bulk update");
            }

            Optional<Product> existingProductOpt = productRepository.findById(dto.getId());
            if (existingProductOpt.isEmpty()) {
                throw new IllegalArgumentException("Product not found with id: " + dto.getId());
            }

            Product existingProduct = existingProductOpt.get();
            updateProductEntity(existingProduct, dto);

            Product savedProduct = productRepository.save(existingProduct);
            updatedProducts.add(convertToDTO(savedProduct));
        }

        return updatedProducts;
    }

    /**
     * Bulk update stock quantities
     */
    public void bulkUpdateStock(Map<Long, Integer> stockUpdates) {
        for (Map.Entry<Long, Integer> entry : stockUpdates.entrySet()) {
            Long productId = entry.getKey();
            Integer newQuantity = entry.getValue();

            if (newQuantity < 0) {
                throw new IllegalArgumentException("Stock quantity cannot be negative for product: " + productId);
            }

            updateStock(productId, newQuantity);
        }
    }
}

