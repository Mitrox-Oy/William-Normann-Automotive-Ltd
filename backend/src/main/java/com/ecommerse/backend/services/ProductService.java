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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service class for managing products
 */
@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    private static final int MAX_SKU_LENGTH = 50;
    private static final int SKU_SUFFIX_LENGTH = 6;
    private static final int SKU_GENERATION_MAX_ATTEMPTS = 12;
    private static final Pattern NON_ALNUM = Pattern.compile("[^A-Z0-9]+");

    @Autowired
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
            CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.categoryService = categoryService;
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
        String searchPattern = toContainsPattern(searchTerm);

        Page<Product> page;
        if (categoryId != null || searchPattern != null) {
            page = productRepository.findActiveForCatalog(categoryId, searchPattern, pageable);
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

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found with id: " + productDTO.getCategoryId()));

        String resolvedProductType = resolveProductType(category);
        String resolvedSku = resolveOrGenerateSkuForCreate(productDTO, resolvedProductType);

        if (productRepository.existsBySku(resolvedSku)) {
            throw new IllegalArgumentException("Product with SKU '" + resolvedSku + "' already exists");
        }

        // Validate publish-critical fields (only on create / when actively publishing).
        validatePublishAttributes(productDTO, resolvedProductType, true, null);

        Product product = convertToEntity(productDTO);
        product.setCategory(category);
        product.setProductType(resolvedProductType);
        product.setSku(resolvedSku);

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

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found with id: " + productDTO.getCategoryId()));

        String resolvedProductType = resolveProductType(category);

        // SKU rules:
        // - Stable after creation (never auto-regenerate).
        // - Admin may manually override by sending a non-blank SKU.
        String incomingSku = trimToNull(productDTO.getSku());
        if (incomingSku != null && !incomingSku.isBlank()) {
            validateManualSku(incomingSku);
            String existingSku = existingProduct.getSku();
            if (existingSku == null || !existingSku.equals(incomingSku)) {
                if (productRepository.existsBySku(incomingSku)) {
                    throw new IllegalArgumentException("Product with SKU '" + incomingSku + "' already exists");
                }
                existingProduct.setSku(incomingSku);
            }
        }

        // Validate publish-critical fields when product is (being) published.
        validatePublishAttributes(productDTO, resolvedProductType, false, existingProduct);

        updateProductEntity(existingProduct, productDTO);
        existingProduct.setCategory(category);
        existingProduct.setProductType(resolvedProductType);

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
        if (productDTO.getPrice() == null) {
            throw new IllegalArgumentException("Product price is required");
        }

        if (productDTO.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be greater than 0");
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
        // SKU is resolved explicitly by createProduct (and preserved on update unless
        // manually overridden).
        // Do not normalize-case SKUs here.
        product.setSku(trimToNull(dto.getSku()));
        product.setImageUrl(dto.getImageUrl());
        product.setActive(dto.getActive() != null ? dto.getActive() : true);
        product.setFeatured(dto.getFeatured() != null ? dto.getFeatured() : false);
        product.setQuoteOnly(dto.getQuoteOnly() != null ? dto.getQuoteOnly() : false);
        product.setWeight(dto.getWeight());
        product.setBrand(dto.getBrand());
        product.setProductType(normalizeExact(dto.getProductType()));
        product.setCondition(normalizeExact(dto.getCondition()));
        product.setOemType(normalizeExact(dto.getOemType()));
        product.setCompatibilityMode(normalizeExact(dto.getCompatibilityMode()));
        product.setCompatibleMakes(listToCsv(dto.getCompatibleMakes()));
        product.setCompatibleModels(listToCsv(dto.getCompatibleModels()));
        product.setCompatibleYearStart(dto.getCompatibleYearStart());
        product.setCompatibleYearEnd(dto.getCompatibleYearEnd());
        product.setVinCompatible(dto.getVinCompatible());
        product.setMake(dto.getMake());
        product.setModel(dto.getModel());
        product.setYear(dto.getYear());
        product.setMileage(dto.getMileage());
        product.setFuelType(normalizeExact(dto.getFuelType()));
        product.setTransmission(normalizeExact(dto.getTransmission()));
        product.setBodyType(normalizeExact(dto.getBodyType()));
        product.setDriveType(normalizeExact(dto.getDriveType()));
        product.setPowerKw(dto.getPowerKw());
        product.setColor(dto.getColor());
        product.setWarrantyIncluded(dto.getWarrantyIncluded());
        product.setPartCategory(dto.getPartCategory());
        product.setPartsMainCategory(normalizeExact(dto.getPartsMainCategory()));
        product.setPartsSubCategory(normalizeExact(dto.getPartsSubCategory()));
        product.setPartsDeepCategory(normalizeExact(dto.getPartsDeepCategory()));
        product.setPartNumber(dto.getPartNumber());
        product.setPartPosition(listToCsv(dto.getPartPosition()));
        product.setWheelDiameterInch(dto.getWheelDiameterInch());
        product.setWheelWidthInch(dto.getWheelWidthInch());
        product.setWheelBoltPattern(normalizeExact(dto.getWheelBoltPattern()));
        product.setWheelOffsetEt(dto.getWheelOffsetEt());
        product.setWheelMaterial(normalizeExact(dto.getWheelMaterial()));
        product.setWheelColor(normalizeExact(dto.getWheelColor()));
        product.setCenterBore(dto.getCenterBore());
        product.setHubCentricRingsNeeded(dto.getHubCentricRingsNeeded());
        product.setEngineType(normalizeExact(dto.getEngineType()));
        product.setEngineDisplacementCc(dto.getEngineDisplacementCc());
        product.setEngineCylinders(dto.getEngineCylinders());
        product.setEnginePowerHp(dto.getEnginePowerHp());
        product.setTurboType(normalizeExact(dto.getTurboType()));
        product.setTurboFlangeType(normalizeExact(dto.getTurboFlangeType()));
        product.setWastegateType(normalizeExact(dto.getWastegateType()));
        product.setRotorDiameterMm(dto.getRotorDiameterMm());
        product.setPadCompound(normalizeExact(dto.getPadCompound()));
        product.setSuspensionAdjustableHeight(dto.getSuspensionAdjustableHeight());
        product.setSuspensionAdjustableDamping(dto.getSuspensionAdjustableDamping());
        product.setLightingVoltage(normalizeExact(dto.getLightingVoltage()));
        product.setBulbType(normalizeExact(dto.getBulbType()));
        product.setMaterial(dto.getMaterial());
        product.setReconditioned(dto.getReconditioned());
        product.setToolCategory(dto.getToolCategory());
        product.setPowerSource(normalizeExact(dto.getPowerSource()));
        product.setVoltage(dto.getVoltage());
        product.setTorqueMinNm(dto.getTorqueMinNm());
        product.setTorqueMaxNm(dto.getTorqueMaxNm());
        product.setDriveSize(normalizeExact(dto.getDriveSize()));
        product.setProfessionalGrade(dto.getProfessionalGrade());
        product.setIsKit(dto.getIsKit());
        product.setCustomCategory(dto.getCustomCategory());
        product.setStyleTags(listToCsv(dto.getStyleTags()));
        product.setFinish(normalizeExact(dto.getFinish()));
        product.setStreetLegal(dto.getStreetLegal());
        product.setInstallationDifficulty(normalizeExact(dto.getInstallationDifficulty()));
        product.setInfoSection1Title(dto.getInfoSection1Title());
        product.setInfoSection1Content(dto.getInfoSection1Content());
        product.setInfoSection1Enabled(
                dto.getInfoSection1Enabled() != null ? dto.getInfoSection1Enabled() : Boolean.FALSE);
        product.setInfoSection2Title(dto.getInfoSection2Title());
        product.setInfoSection2Content(dto.getInfoSection2Content());
        product.setInfoSection2Enabled(
                dto.getInfoSection2Enabled() != null ? dto.getInfoSection2Enabled() : Boolean.FALSE);
        product.setInfoSection3Title(dto.getInfoSection3Title());
        product.setInfoSection3Content(dto.getInfoSection3Content());
        product.setInfoSection3Enabled(
                dto.getInfoSection3Enabled() != null ? dto.getInfoSection3Enabled() : Boolean.FALSE);
        product.setInfoSection4Title(dto.getInfoSection4Title());
        product.setInfoSection4Content(dto.getInfoSection4Content());
        product.setInfoSection4Enabled(
                dto.getInfoSection4Enabled() != null ? dto.getInfoSection4Enabled() : Boolean.FALSE);
        product.setInfoSection5Title(dto.getInfoSection5Title());
        product.setInfoSection5Content(dto.getInfoSection5Content());
        product.setInfoSection5Enabled(
                dto.getInfoSection5Enabled() != null ? dto.getInfoSection5Enabled() : Boolean.FALSE);
        product.setInfoSection6Title(dto.getInfoSection6Title());
        product.setInfoSection6Content(dto.getInfoSection6Content());
        product.setInfoSection6Enabled(
                dto.getInfoSection6Enabled() != null ? dto.getInfoSection6Enabled() : Boolean.FALSE);
        product.setInfoSection7Title(dto.getInfoSection7Title());
        product.setInfoSection7Content(dto.getInfoSection7Content());
        product.setInfoSection7Enabled(
                dto.getInfoSection7Enabled() != null ? dto.getInfoSection7Enabled() : Boolean.FALSE);
        product.setInfoSection8Title(dto.getInfoSection8Title());
        product.setInfoSection8Content(dto.getInfoSection8Content());
        product.setInfoSection8Enabled(
                dto.getInfoSection8Enabled() != null ? dto.getInfoSection8Enabled() : Boolean.FALSE);
        product.setInfoSection9Title(dto.getInfoSection9Title());
        product.setInfoSection9Content(dto.getInfoSection9Content());
        product.setInfoSection9Enabled(
                dto.getInfoSection9Enabled() != null ? dto.getInfoSection9Enabled() : Boolean.FALSE);
        product.setInfoSection10Title(dto.getInfoSection10Title());
        product.setInfoSection10Content(dto.getInfoSection10Content());
        product.setInfoSection10Enabled(
                dto.getInfoSection10Enabled() != null ? dto.getInfoSection10Enabled() : Boolean.FALSE);
        return product;
    }

    private void updateProductEntity(Product product, ProductDTO dto) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0);
        // Do not auto-regenerate or clear SKU on update. Manual SKU overrides are
        // handled in updateProduct().
        product.setImageUrl(dto.getImageUrl());
        product.setActive(dto.getActive() != null ? dto.getActive() : true);
        product.setFeatured(dto.getFeatured() != null ? dto.getFeatured() : false);
        product.setQuoteOnly(dto.getQuoteOnly() != null ? dto.getQuoteOnly() : false);
        product.setWeight(dto.getWeight());
        product.setBrand(dto.getBrand());
        product.setProductType(normalizeExact(dto.getProductType()));
        product.setCondition(normalizeExact(dto.getCondition()));
        product.setOemType(normalizeExact(dto.getOemType()));
        product.setCompatibilityMode(normalizeExact(dto.getCompatibilityMode()));
        product.setCompatibleMakes(listToCsv(dto.getCompatibleMakes()));
        product.setCompatibleModels(listToCsv(dto.getCompatibleModels()));
        product.setCompatibleYearStart(dto.getCompatibleYearStart());
        product.setCompatibleYearEnd(dto.getCompatibleYearEnd());
        product.setVinCompatible(dto.getVinCompatible());
        product.setMake(dto.getMake());
        product.setModel(dto.getModel());
        product.setYear(dto.getYear());
        product.setMileage(dto.getMileage());
        product.setFuelType(normalizeExact(dto.getFuelType()));
        product.setTransmission(normalizeExact(dto.getTransmission()));
        product.setBodyType(normalizeExact(dto.getBodyType()));
        product.setDriveType(normalizeExact(dto.getDriveType()));
        product.setPowerKw(dto.getPowerKw());
        product.setColor(dto.getColor());
        product.setWarrantyIncluded(dto.getWarrantyIncluded());
        product.setPartCategory(dto.getPartCategory());
        product.setPartsMainCategory(normalizeExact(dto.getPartsMainCategory()));
        product.setPartsSubCategory(normalizeExact(dto.getPartsSubCategory()));
        product.setPartsDeepCategory(normalizeExact(dto.getPartsDeepCategory()));
        product.setPartNumber(dto.getPartNumber());
        product.setPartPosition(listToCsv(dto.getPartPosition()));
        product.setWheelDiameterInch(dto.getWheelDiameterInch());
        product.setWheelWidthInch(dto.getWheelWidthInch());
        product.setWheelBoltPattern(normalizeExact(dto.getWheelBoltPattern()));
        product.setWheelOffsetEt(dto.getWheelOffsetEt());
        product.setWheelMaterial(normalizeExact(dto.getWheelMaterial()));
        product.setWheelColor(normalizeExact(dto.getWheelColor()));
        product.setCenterBore(dto.getCenterBore());
        product.setHubCentricRingsNeeded(dto.getHubCentricRingsNeeded());
        product.setEngineType(normalizeExact(dto.getEngineType()));
        product.setEngineDisplacementCc(dto.getEngineDisplacementCc());
        product.setEngineCylinders(dto.getEngineCylinders());
        product.setEnginePowerHp(dto.getEnginePowerHp());
        product.setTurboType(normalizeExact(dto.getTurboType()));
        product.setTurboFlangeType(normalizeExact(dto.getTurboFlangeType()));
        product.setWastegateType(normalizeExact(dto.getWastegateType()));
        product.setRotorDiameterMm(dto.getRotorDiameterMm());
        product.setPadCompound(normalizeExact(dto.getPadCompound()));
        product.setSuspensionAdjustableHeight(dto.getSuspensionAdjustableHeight());
        product.setSuspensionAdjustableDamping(dto.getSuspensionAdjustableDamping());
        product.setLightingVoltage(normalizeExact(dto.getLightingVoltage()));
        product.setBulbType(normalizeExact(dto.getBulbType()));
        product.setMaterial(dto.getMaterial());
        product.setReconditioned(dto.getReconditioned());
        product.setToolCategory(dto.getToolCategory());
        product.setPowerSource(normalizeExact(dto.getPowerSource()));
        product.setVoltage(dto.getVoltage());
        product.setTorqueMinNm(dto.getTorqueMinNm());
        product.setTorqueMaxNm(dto.getTorqueMaxNm());
        product.setDriveSize(normalizeExact(dto.getDriveSize()));
        product.setProfessionalGrade(dto.getProfessionalGrade());
        product.setIsKit(dto.getIsKit());
        product.setCustomCategory(dto.getCustomCategory());
        product.setStyleTags(listToCsv(dto.getStyleTags()));
        product.setFinish(normalizeExact(dto.getFinish()));
        product.setStreetLegal(dto.getStreetLegal());
        product.setInstallationDifficulty(normalizeExact(dto.getInstallationDifficulty()));
        product.setInfoSection1Title(dto.getInfoSection1Title());
        product.setInfoSection1Content(dto.getInfoSection1Content());
        product.setInfoSection1Enabled(
                dto.getInfoSection1Enabled() != null ? dto.getInfoSection1Enabled() : Boolean.FALSE);
        product.setInfoSection2Title(dto.getInfoSection2Title());
        product.setInfoSection2Content(dto.getInfoSection2Content());
        product.setInfoSection2Enabled(
                dto.getInfoSection2Enabled() != null ? dto.getInfoSection2Enabled() : Boolean.FALSE);
        product.setInfoSection3Title(dto.getInfoSection3Title());
        product.setInfoSection3Content(dto.getInfoSection3Content());
        product.setInfoSection3Enabled(
                dto.getInfoSection3Enabled() != null ? dto.getInfoSection3Enabled() : Boolean.FALSE);
        product.setInfoSection4Title(dto.getInfoSection4Title());
        product.setInfoSection4Content(dto.getInfoSection4Content());
        product.setInfoSection4Enabled(
                dto.getInfoSection4Enabled() != null ? dto.getInfoSection4Enabled() : Boolean.FALSE);
        product.setInfoSection5Title(dto.getInfoSection5Title());
        product.setInfoSection5Content(dto.getInfoSection5Content());
        product.setInfoSection5Enabled(
                dto.getInfoSection5Enabled() != null ? dto.getInfoSection5Enabled() : Boolean.FALSE);
        product.setInfoSection6Title(dto.getInfoSection6Title());
        product.setInfoSection6Content(dto.getInfoSection6Content());
        product.setInfoSection6Enabled(
                dto.getInfoSection6Enabled() != null ? dto.getInfoSection6Enabled() : Boolean.FALSE);
        product.setInfoSection7Title(dto.getInfoSection7Title());
        product.setInfoSection7Content(dto.getInfoSection7Content());
        product.setInfoSection7Enabled(
                dto.getInfoSection7Enabled() != null ? dto.getInfoSection7Enabled() : Boolean.FALSE);
        product.setInfoSection8Title(dto.getInfoSection8Title());
        product.setInfoSection8Content(dto.getInfoSection8Content());
        product.setInfoSection8Enabled(
                dto.getInfoSection8Enabled() != null ? dto.getInfoSection8Enabled() : Boolean.FALSE);
        product.setInfoSection9Title(dto.getInfoSection9Title());
        product.setInfoSection9Content(dto.getInfoSection9Content());
        product.setInfoSection9Enabled(
                dto.getInfoSection9Enabled() != null ? dto.getInfoSection9Enabled() : Boolean.FALSE);
        product.setInfoSection10Title(dto.getInfoSection10Title());
        product.setInfoSection10Content(dto.getInfoSection10Content());
        product.setInfoSection10Enabled(
                dto.getInfoSection10Enabled() != null ? dto.getInfoSection10Enabled() : Boolean.FALSE);
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
        dto.setQuoteOnly(product.getQuoteOnly());
        dto.setWeight(product.getWeight());
        dto.setBrand(product.getBrand());
        dto.setProductType(product.getProductType());
        dto.setCondition(product.getCondition());
        dto.setOemType(product.getOemType());
        dto.setCompatibilityMode(product.getCompatibilityMode());
        dto.setCompatibleMakes(csvToList(product.getCompatibleMakes()));
        dto.setCompatibleModels(csvToList(product.getCompatibleModels()));
        dto.setCompatibleYearStart(product.getCompatibleYearStart());
        dto.setCompatibleYearEnd(product.getCompatibleYearEnd());
        dto.setVinCompatible(product.getVinCompatible());
        dto.setMake(product.getMake());
        dto.setModel(product.getModel());
        dto.setYear(product.getYear());
        dto.setMileage(product.getMileage());
        dto.setFuelType(product.getFuelType());
        dto.setTransmission(product.getTransmission());
        dto.setBodyType(product.getBodyType());
        dto.setDriveType(product.getDriveType());
        dto.setPowerKw(product.getPowerKw());
        dto.setColor(product.getColor());
        dto.setWarrantyIncluded(product.getWarrantyIncluded());
        dto.setPartCategory(product.getPartCategory());
        dto.setPartsMainCategory(product.getPartsMainCategory());
        dto.setPartsSubCategory(product.getPartsSubCategory());
        dto.setPartsDeepCategory(product.getPartsDeepCategory());
        dto.setPartNumber(product.getPartNumber());
        dto.setPartPosition(csvToList(product.getPartPosition()));
        dto.setWheelDiameterInch(product.getWheelDiameterInch());
        dto.setWheelWidthInch(product.getWheelWidthInch());
        dto.setWheelBoltPattern(product.getWheelBoltPattern());
        dto.setWheelOffsetEt(product.getWheelOffsetEt());
        dto.setWheelMaterial(product.getWheelMaterial());
        dto.setWheelColor(product.getWheelColor());
        dto.setCenterBore(product.getCenterBore());
        dto.setHubCentricRingsNeeded(product.getHubCentricRingsNeeded());
        dto.setEngineType(product.getEngineType());
        dto.setEngineDisplacementCc(product.getEngineDisplacementCc());
        dto.setEngineCylinders(product.getEngineCylinders());
        dto.setEnginePowerHp(product.getEnginePowerHp());
        dto.setTurboType(product.getTurboType());
        dto.setTurboFlangeType(product.getTurboFlangeType());
        dto.setWastegateType(product.getWastegateType());
        dto.setRotorDiameterMm(product.getRotorDiameterMm());
        dto.setPadCompound(product.getPadCompound());
        dto.setSuspensionAdjustableHeight(product.getSuspensionAdjustableHeight());
        dto.setSuspensionAdjustableDamping(product.getSuspensionAdjustableDamping());
        dto.setLightingVoltage(product.getLightingVoltage());
        dto.setBulbType(product.getBulbType());
        dto.setMaterial(product.getMaterial());
        dto.setReconditioned(product.getReconditioned());
        dto.setToolCategory(product.getToolCategory());
        dto.setPowerSource(product.getPowerSource());
        dto.setVoltage(product.getVoltage());
        dto.setTorqueMinNm(product.getTorqueMinNm());
        dto.setTorqueMaxNm(product.getTorqueMaxNm());
        dto.setDriveSize(product.getDriveSize());
        dto.setProfessionalGrade(product.getProfessionalGrade());
        dto.setIsKit(product.getIsKit());
        dto.setCustomCategory(product.getCustomCategory());
        dto.setStyleTags(csvToList(product.getStyleTags()));
        dto.setFinish(product.getFinish());
        dto.setStreetLegal(product.getStreetLegal());
        dto.setInstallationDifficulty(product.getInstallationDifficulty());
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
        ProductFilterCriteria criteria = new ProductFilterCriteria();
        criteria.setMinPrice(minPrice);
        criteria.setMaxPrice(maxPrice);
        criteria.setBrand(brand);
        criteria.setInStockOnly(inStockOnly);
        return searchWithFilters(null, query, categoryId, criteria, featuredOnly, pageable);
    }

    private String resolveOrGenerateSkuForCreate(ProductDTO dto, String resolvedProductType) {
        String incomingSku = trimToNull(dto.getSku());
        if (incomingSku != null && !incomingSku.isBlank()) {
            validateManualSku(incomingSku);
            return incomingSku;
        }

        String generated = generateSku(dto, resolvedProductType);
        // Ensure uniqueness. Extremely unlikely to loop, but enforce deterministically.
        for (int attempt = 0; attempt < SKU_GENERATION_MAX_ATTEMPTS; attempt++) {
            if (!productRepository.existsBySku(generated)) {
                // Store back on DTO so callers/admin UI can display it after save.
                dto.setSku(generated);
                return generated;
            }
            generated = bumpSkuSuffix(generated);
        }
        throw new IllegalStateException(
                "Failed to generate a unique SKU after " + SKU_GENERATION_MAX_ATTEMPTS + " attempts");
    }

    /**
     * SKU auto-generation:
     * - Derived from product snapshot data at creation time (human-friendly).
     * - Uniqueness guaranteed via random suffix.
     * - Stable: never auto-regenerates after creation.
     */
    public String generateSku(ProductDTO dto, String resolvedProductType) {
        String type = resolvedProductType != null ? resolvedProductType.trim().toLowerCase(Locale.ROOT) : "";

        String derived;
        if ("part".equals(type)) {
            // Example: PART-OIL-FILTER-FORD-ABC123
            String category = abbreviateWord(dto.getPartCategory(), 12);
            String makeOrBrand = abbreviateWord(firstNonBlank(dto.getMake(), dto.getBrand()), 8);
            String nameHint = abbreviateWords(dto.getName(), 2, 10);

            derived = joinSkuSegments(
                    "PART",
                    firstNonBlank(category, nameHint),
                    makeOrBrand);
        } else if ("car".equals(type)) {
            // Example: MERC-AMG-C63-S-2020-ABC123
            String make = abbreviateWord(dto.getMake(), 4);
            String model = abbreviateWord(dto.getModel(), 10);
            String nameHint = abbreviateWords(dto.getName(), 4, 10);
            String year = dto.getYear() != null ? String.valueOf(dto.getYear()) : null;

            derived = joinSkuSegments(
                    firstNonBlank(make, nameHint),
                    model,
                    year);
        } else {
            // Fallback for tools/custom/etc.
            String nameHint = abbreviateWords(dto.getName(), 5, 12);
            derived = firstNonBlank(nameHint, "PROD");
        }

        String base = normalizeSkuBase(derived);
        if (base.isBlank()) {
            base = "PROD";
        }

        String suffix = randomSkuSuffix();
        int maxBaseLen = Math.max(1, MAX_SKU_LENGTH - (1 + SKU_SUFFIX_LENGTH));
        if (base.length() > maxBaseLen) {
            base = base.substring(0, maxBaseLen).replaceAll("-+$", "");
            if (base.isBlank())
                base = "PROD";
        }

        return base + "-" + suffix;
    }

    private String bumpSkuSuffix(String sku) {
        // Replace suffix after last '-' (or append if missing).
        String suffix = randomSkuSuffix();
        int idx = sku.lastIndexOf('-');
        String base = idx > 0 ? sku.substring(0, idx) : sku;
        int maxBaseLen = Math.max(1, MAX_SKU_LENGTH - (1 + SKU_SUFFIX_LENGTH));
        if (base.length() > maxBaseLen) {
            base = base.substring(0, maxBaseLen).replaceAll("-+$", "");
            if (base.isBlank())
                base = "PROD";
        }
        return base + "-" + suffix;
    }

    private void validateManualSku(String sku) {
        String trimmed = sku.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be blank");
        }
        if (trimmed.length() < 3) {
            throw new IllegalArgumentException("SKU must be at least 3 characters");
        }
        if (trimmed.length() > MAX_SKU_LENGTH) {
            throw new IllegalArgumentException("SKU must be at most " + MAX_SKU_LENGTH + " characters");
        }
    }

    private void validatePublishAttributes(ProductDTO dto, String resolvedProductType, boolean isCreate,
            Product existingProduct) {
        // Only enforce completeness when product is being published.
        boolean willBeActive = dto.getActive() != null ? Boolean.TRUE.equals(dto.getActive())
                : (existingProduct != null ? Boolean.TRUE.equals(existingProduct.getActive()) : true);

        if (!willBeActive)
            return;
        if (resolvedProductType == null)
            return;

        String type = resolvedProductType.trim().toLowerCase(Locale.ROOT);
        if ("car".equals(type)) {
            // Avoid breaking legacy updates: enforce on create, and on updates only when
            // explicit active=true is sent.
            boolean enforce = isCreate || (dto.getActive() != null && Boolean.TRUE.equals(dto.getActive()));
            if (!enforce)
                return;

            if (isBlank(dto.getMake()) || isBlank(dto.getModel()) || dto.getYear() == null) {
                throw new IllegalArgumentException("Cars require make, model and year when active");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String... values) {
        if (values == null)
            return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty())
                return v.trim();
        }
        return null;
    }

    private static String randomSkuSuffix() {
        // Uppercase hex (A-F0-9) is fine for uniqueness and DB constraints.
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return raw.substring(0, SKU_SUFFIX_LENGTH);
    }

    private static String normalizeSkuBase(String value) {
        if (value == null)
            return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        normalized = normalized.toUpperCase(Locale.ROOT);
        normalized = NON_ALNUM.matcher(normalized).replaceAll("-");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("^-+", "").replaceAll("-+$", "");
        return normalized;
    }

    private static String joinSkuSegments(String... segments) {
        return Arrays.stream(segments)
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.joining("-"));
    }

    private static String abbreviateWord(String value, int maxLen) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty())
            return null;
        // Keep alnum chunks only for abbreviation logic; normalize later.
        String chunk = trimmed.replaceAll("[^A-Za-z0-9]+", "");
        if (chunk.isEmpty())
            chunk = trimmed;
        if (chunk.length() <= maxLen)
            return chunk;
        return chunk.substring(0, maxLen);
    }

    private static String abbreviateWords(String value, int maxWords, int maxWordLen) {
        if (value == null)
            return null;
        String[] parts = value.trim().split("\\s+");
        if (parts.length == 0)
            return null;
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (String part : parts) {
            if (part == null)
                continue;
            String p = part.trim();
            if (p.isEmpty())
                continue;
            if (used >= maxWords)
                break;
            String abbr = abbreviateWord(p, maxWordLen);
            if (abbr == null || abbr.isEmpty())
                continue;
            if (sb.length() > 0)
                sb.append("-");
            sb.append(abbr);
            used++;
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * Get products scoped to a root category (topic) and all its descendants
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByRootCategory(Long rootCategoryId, String searchTerm, Pageable pageable) {
        List<Long> categoryIds = categoryService.getAllDescendantCategoryIds(rootCategoryId);
        String searchPattern = toContainsPattern(searchTerm);
        return productRepository.findByCategoryIdsAndSearch(categoryIds, searchPattern, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Advanced search scoped to a root category (topic) and all its descendants
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> advancedSearchByRootCategory(Long rootCategoryId, String query, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice, String brand, Boolean inStockOnly,
            Boolean featuredOnly, Pageable pageable) {
        ProductFilterCriteria criteria = new ProductFilterCriteria();
        criteria.setMinPrice(minPrice);
        criteria.setMaxPrice(maxPrice);
        criteria.setBrand(brand);
        criteria.setInStockOnly(inStockOnly);
        return searchWithFilters(rootCategoryId, query, categoryId, criteria, featuredOnly, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> searchWithFilters(Long rootCategoryId, String query, Long categoryId,
            ProductFilterCriteria criteria, Boolean featuredOnly, Pageable pageable) {
        ProductFilterCriteria resolved = criteria != null ? criteria : new ProductFilterCriteria();

        String queryPattern = toContainsPattern(query);
        String brandPattern = toContainsPattern(resolved.getBrand());
        String conditionValue = normalizeExact(resolved.getCondition());
        String productTypeValue = normalizeExact(resolved.getProductType());

        String makePattern = toContainsPattern(resolved.getMake());
        String modelPattern = toContainsPattern(resolved.getModel());

        String fuelTypeValue = normalizeExact(resolved.getFuelType());
        String transmissionValue = normalizeExact(resolved.getTransmission());
        String bodyTypeValue = normalizeExact(resolved.getBodyType());
        String driveTypeValue = normalizeExact(resolved.getDriveType());

        String compatibilityModeValue = normalizeExact(resolved.getCompatibilityMode());
        String compatibleMakePattern = toCsvTokenPattern(resolved.getCompatibleMake());
        String compatibleModelPattern = toCsvTokenPattern(resolved.getCompatibleModel());
        String oemTypeValue = normalizeExact(resolved.getOemType());

        String partCategoryPattern = toContainsPattern(resolved.getPartCategory());
        String partsMainCategoryValue = normalizeExact(resolved.getPartsMainCategory());
        String partsSubCategoryValue = normalizeExact(resolved.getPartsSubCategory());
        String partsDeepCategoryValue = normalizeExact(resolved.getPartsDeepCategory());
        String partNumberPattern = toContainsPattern(resolved.getPartNumber());
        String partPositionPattern = toCsvTokenPattern(firstValue(resolved.getPartPosition()));

        String wheelBoltPatternValue = normalizeExact(resolved.getWheelBoltPattern());
        String wheelMaterialValue = normalizeExact(resolved.getWheelMaterial());
        String wheelColorValue = normalizeExact(resolved.getWheelColor());
        String engineTypeValue = normalizeExact(resolved.getEngineType());
        String turboTypeValue = normalizeExact(resolved.getTurboType());
        String flangeTypeValue = normalizeExact(resolved.getFlangeType());
        String wastegateTypeValue = normalizeExact(resolved.getWastegateType());
        String padCompoundValue = normalizeExact(resolved.getPadCompound());
        String lightingVoltageValue = normalizeExact(resolved.getLightingVoltage());
        String bulbTypeValue = normalizeExact(resolved.getBulbType());

        String toolCategoryPattern = toContainsPattern(resolved.getToolCategory());
        String powerSourceValue = normalizeExact(resolved.getPowerSource());
        String driveSizeValue = normalizeExact(resolved.getDriveSize());

        String styleTagPattern = toCsvTokenPattern(firstValue(resolved.getStyleTags()));
        String finishValue = normalizeExact(resolved.getFinish());
        String installationDifficultyValue = normalizeExact(resolved.getInstallationDifficulty());
        String customCategoryPattern = toContainsPattern(resolved.getCustomCategory());

        boolean inStockOnly = Boolean.TRUE.equals(resolved.getInStockOnly());
        boolean featuredOnlyResolved = Boolean.TRUE.equals(featuredOnly);

        Page<Product> products;
        if (rootCategoryId != null) {
            List<Long> categoryIds = categoryService.getAllDescendantCategoryIds(rootCategoryId);
            products = productRepository.findWithFiltersAndRootScope(categoryIds, queryPattern, categoryId,
                    resolved.getMinPrice(), resolved.getMaxPrice(), brandPattern,
                    conditionValue, productTypeValue, makePattern, modelPattern,
                    resolved.getYearMin(), resolved.getYearMax(), resolved.getMileageMin(), resolved.getMileageMax(),
                    fuelTypeValue, transmissionValue, bodyTypeValue, driveTypeValue,
                    resolved.getPowerMin(), resolved.getPowerMax(), resolved.getWarrantyIncluded(),
                    compatibilityModeValue, compatibleMakePattern, compatibleModelPattern, resolved.getCompatibleYear(),
                    oemTypeValue, partCategoryPattern, partsMainCategoryValue, partsSubCategoryValue,
                    partsDeepCategoryValue,
                    partNumberPattern, partPositionPattern,
                    resolved.getWheelDiameterMin(), resolved.getWheelDiameterMax(),
                    resolved.getWheelWidthMin(), resolved.getWheelWidthMax(),
                    resolved.getWheelOffsetMin(), resolved.getWheelOffsetMax(),
                    resolved.getCenterBoreMin(), resolved.getCenterBoreMax(),
                    wheelBoltPatternValue, wheelMaterialValue, wheelColorValue, resolved.getHubCentricRingsNeeded(),
                    engineTypeValue, resolved.getEngineDisplacementMin(), resolved.getEngineDisplacementMax(),
                    resolved.getEngineCylinders(), resolved.getEnginePowerMin(), resolved.getEnginePowerMax(),
                    turboTypeValue, flangeTypeValue, wastegateTypeValue,
                    resolved.getRotorDiameterMin(), resolved.getRotorDiameterMax(),
                    padCompoundValue, resolved.getAdjustableHeight(), resolved.getAdjustableDamping(),
                    lightingVoltageValue, bulbTypeValue,
                    toolCategoryPattern, powerSourceValue, resolved.getVoltageMin(), resolved.getVoltageMax(),
                    resolved.getTorqueMin(), resolved.getTorqueMax(), driveSizeValue,
                    resolved.getProfessionalGrade(), resolved.getIsKit(),
                    styleTagPattern, finishValue, resolved.getStreetLegal(),
                    installationDifficultyValue, customCategoryPattern,
                    inStockOnly, featuredOnlyResolved, pageable);
        } else {
            products = productRepository.findWithFilters(queryPattern, categoryId,
                    resolved.getMinPrice(), resolved.getMaxPrice(), brandPattern,
                    conditionValue, productTypeValue, makePattern, modelPattern,
                    resolved.getYearMin(), resolved.getYearMax(), resolved.getMileageMin(), resolved.getMileageMax(),
                    fuelTypeValue, transmissionValue, bodyTypeValue, driveTypeValue,
                    resolved.getPowerMin(), resolved.getPowerMax(), resolved.getWarrantyIncluded(),
                    compatibilityModeValue, compatibleMakePattern, compatibleModelPattern, resolved.getCompatibleYear(),
                    oemTypeValue, partCategoryPattern, partsMainCategoryValue, partsSubCategoryValue,
                    partsDeepCategoryValue,
                    partNumberPattern, partPositionPattern,
                    resolved.getWheelDiameterMin(), resolved.getWheelDiameterMax(),
                    resolved.getWheelWidthMin(), resolved.getWheelWidthMax(),
                    resolved.getWheelOffsetMin(), resolved.getWheelOffsetMax(),
                    resolved.getCenterBoreMin(), resolved.getCenterBoreMax(),
                    wheelBoltPatternValue, wheelMaterialValue, wheelColorValue, resolved.getHubCentricRingsNeeded(),
                    engineTypeValue, resolved.getEngineDisplacementMin(), resolved.getEngineDisplacementMax(),
                    resolved.getEngineCylinders(), resolved.getEnginePowerMin(), resolved.getEnginePowerMax(),
                    turboTypeValue, flangeTypeValue, wastegateTypeValue,
                    resolved.getRotorDiameterMin(), resolved.getRotorDiameterMax(),
                    padCompoundValue, resolved.getAdjustableHeight(), resolved.getAdjustableDamping(),
                    lightingVoltageValue, bulbTypeValue,
                    toolCategoryPattern, powerSourceValue, resolved.getVoltageMin(), resolved.getVoltageMax(),
                    resolved.getTorqueMin(), resolved.getTorqueMax(), driveSizeValue,
                    resolved.getProfessionalGrade(), resolved.getIsKit(),
                    styleTagPattern, finishValue, resolved.getStreetLegal(),
                    installationDifficultyValue, customCategoryPattern,
                    inStockOnly, featuredOnlyResolved, pageable);
        }

        return products.map(this::convertToDTO);
    }

    /**
     * Get brands scoped to a root category (topic) and all its descendants
     */
    @Transactional(readOnly = true)
    public List<String> getBrandsByRootCategory(Long rootCategoryId) {
        List<Long> categoryIds = categoryService.getAllDescendantCategoryIds(rootCategoryId);
        return productRepository.findBrandsByCategoryIds(categoryIds);
    }

    /**
     * Get all distinct product brands
     */
    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        return productRepository.findAllBrands();
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

    private String toContainsPattern(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return "%" + value.trim().toLowerCase() + "%";
    }

    private String toCsvTokenPattern(String value) {
        String normalized = normalizeExact(value);
        if (normalized == null) {
            return null;
        }
        return "%," + normalized + ",%";
    }

    private String normalizeExact(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> csvToList(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

    private String listToCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    private String firstValue(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
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
