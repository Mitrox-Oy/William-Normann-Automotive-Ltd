package com.ecommerse.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Product
 */
@Schema(description = "Product information")
public class ProductDTO {

    @Schema(description = "Product ID", example = "1")
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    @Schema(description = "Product name", example = "iPhone 15 Pro", required = true)
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Schema(description = "Product description", example = "Latest iPhone with advanced camera system")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be at least 0")
    @Schema(description = "Product price", example = "999.99", required = true)
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Schema(description = "Available stock quantity", example = "50")
    private Integer stockQuantity;

    // SKU is optional on create. If omitted/blank, backend auto-generates a stable SKU.
    // If provided, backend enforces uniqueness and length constraints.
    @Size(max = 50, message = "SKU must be at most 50 characters")
    @Schema(description = "Stock Keeping Unit (optional on create; auto-generated if blank)", example = "IPH15P-256-BLK", required = false)
    private String sku;

    @Schema(description = "Product image URL", example = "https://example.com/images/iphone15pro.jpg")
    private String imageUrl;

    @Schema(description = "Whether the product is active", example = "true")
    private Boolean active;

    @Schema(description = "Whether the product is featured", example = "false")
    private Boolean featured;

    @Schema(description = "If true, product cannot be directly added to cart and must be purchased via quote request", example = "false")
    private Boolean quoteOnly;

    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    @Schema(description = "Product weight in kg", example = "0.221")
    private BigDecimal weight;

    @Size(max = 100, message = "Brand name cannot exceed 100 characters")
    @Schema(description = "Product brand", example = "Apple")
    private String brand;

    @Schema(description = "Denormalized product type from root topic", example = "car")
    private String productType;

    @Schema(description = "Product condition", example = "used")
    private String condition;

    @Schema(description = "OEM classification for parts/custom", example = "oem")
    private String oemType;

    @Schema(description = "Compatibility mode", example = "vehicle_specific")
    private String compatibilityMode;

    @Schema(description = "Compatible vehicle makes")
    private List<String> compatibleMakes;

    @Schema(description = "Compatible vehicle models")
    private List<String> compatibleModels;

    @Schema(description = "Compatibility year range start", example = "2015")
    private Integer compatibleYearStart;

    @Schema(description = "Compatibility year range end", example = "2020")
    private Integer compatibleYearEnd;

    @Schema(description = "Whether VIN compatibility is supported", example = "true")
    private Boolean vinCompatible;

    @Schema(description = "Car make", example = "BMW")
    private String make;

    @Schema(description = "Car model", example = "330i")
    private String model;

    @Schema(description = "Model year", example = "2018")
    private Integer year;

    @Schema(description = "Mileage for cars", example = "110000")
    private Integer mileage;

    @Schema(description = "Fuel type", example = "diesel")
    private String fuelType;

    @Schema(description = "Transmission type", example = "automatic")
    private String transmission;

    @Schema(description = "Body type", example = "sedan")
    private String bodyType;

    @Schema(description = "Drive type", example = "awd")
    private String driveType;

    @Schema(description = "Power in kW", example = "165")
    private Integer powerKw;

    @Schema(description = "Vehicle color", example = "Black")
    private String color;

    @Schema(description = "Warranty included", example = "false")
    private Boolean warrantyIncluded;

    @Schema(description = "Part category", example = "brakes")
    private String partCategory;

    @Schema(description = "OEM part number", example = "34-11-6-789-101")
    private String partNumber;

    @Schema(description = "Part position tags")
    private List<String> partPosition;

    @Schema(description = "Part material", example = "aluminum")
    private String material;

    @Schema(description = "Part is reconditioned", example = "false")
    private Boolean reconditioned;

    @Schema(description = "Tool category", example = "torque_wrench")
    private String toolCategory;

    @Schema(description = "Tool power source", example = "battery")
    private String powerSource;

    @Schema(description = "Tool voltage", example = "18")
    private Integer voltage;

    @Schema(description = "Minimum torque (Nm)", example = "20")
    private Integer torqueMinNm;

    @Schema(description = "Maximum torque (Nm)", example = "200")
    private Integer torqueMaxNm;

    @Schema(description = "Drive size", example = "1/2\"")
    private String driveSize;

    @Schema(description = "Professional grade tool", example = "true")
    private Boolean professionalGrade;

    @Schema(description = "Tool sold as kit", example = "false")
    private Boolean isKit;

    @Schema(description = "Custom category", example = "body_kit")
    private String customCategory;

    @Schema(description = "Style tags for custom products")
    private List<String> styleTags;

    @Schema(description = "Finish type", example = "matte")
    private String finish;

    @Schema(description = "Street legal", example = "true")
    private Boolean streetLegal;

    @Schema(description = "Installation difficulty", example = "medium")
    private String installationDifficulty;

    @Schema(description = "Optional info section 1 title", example = "Product Info")
    private String infoSection1Title;

    @Schema(description = "Optional info section 1 content")
    private String infoSection1Content;

    @Schema(description = "Whether info section 1 is shown", example = "true")
    private Boolean infoSection1Enabled;

    @Schema(description = "Optional info section 2 title", example = "Warranty")
    private String infoSection2Title;

    @Schema(description = "Optional info section 2 content")
    private String infoSection2Content;

    @Schema(description = "Whether info section 2 is shown", example = "false")
    private Boolean infoSection2Enabled;

    @Schema(description = "Optional info section 3 title", example = "Service")
    private String infoSection3Title;

    @Schema(description = "Optional info section 3 content")
    private String infoSection3Content;

    @Schema(description = "Whether info section 3 is shown", example = "false")
    private Boolean infoSection3Enabled;

    @Schema(description = "Optional info section 4 title")
    private String infoSection4Title;

    @Schema(description = "Optional info section 4 content")
    private String infoSection4Content;

    @Schema(description = "Whether info section 4 is shown", example = "false")
    private Boolean infoSection4Enabled;

    @Schema(description = "Optional info section 5 title")
    private String infoSection5Title;

    @Schema(description = "Optional info section 5 content")
    private String infoSection5Content;

    @Schema(description = "Whether info section 5 is shown", example = "false")
    private Boolean infoSection5Enabled;

    @Schema(description = "Optional info section 6 title")
    private String infoSection6Title;

    @Schema(description = "Optional info section 6 content")
    private String infoSection6Content;

    @Schema(description = "Whether info section 6 is shown", example = "false")
    private Boolean infoSection6Enabled;

    @Schema(description = "Optional info section 7 title")
    private String infoSection7Title;

    @Schema(description = "Optional info section 7 content")
    private String infoSection7Content;

    @Schema(description = "Whether info section 7 is shown", example = "false")
    private Boolean infoSection7Enabled;

    @Schema(description = "Optional info section 8 title")
    private String infoSection8Title;

    @Schema(description = "Optional info section 8 content")
    private String infoSection8Content;

    @Schema(description = "Whether info section 8 is shown", example = "false")
    private Boolean infoSection8Enabled;

    @Schema(description = "Optional info section 9 title")
    private String infoSection9Title;

    @Schema(description = "Optional info section 9 content")
    private String infoSection9Content;

    @Schema(description = "Whether info section 9 is shown", example = "false")
    private Boolean infoSection9Enabled;

    @Schema(description = "Optional info section 10 title")
    private String infoSection10Title;

    @Schema(description = "Optional info section 10 content")
    private String infoSection10Content;

    @Schema(description = "Whether info section 10 is shown", example = "false")
    private Boolean infoSection10Enabled;

    @NotNull(message = "Category is required")
    @Schema(description = "Category ID", example = "1", required = true)
    private Long categoryId;

    @Schema(description = "Category name", example = "Smartphones")
    private String categoryName;

    @Schema(description = "Whether the product is in stock", example = "true")
    private Boolean inStock;

    @Schema(description = "Whether the product is available for purchase", example = "true")
    private Boolean available;

    @Schema(description = "Product creation date")
    private LocalDateTime createdDate;

    @Schema(description = "Product last update date")
    private LocalDateTime updatedDate;

    @Schema(description = "Product images")
    private java.util.List<ProductImageResponse> images;

    @Schema(description = "Available product variants")
    private java.util.List<ProductVariantResponse> variants;

    // Constructors
    public ProductDTO() {
    }

    public ProductDTO(Long id, String name, BigDecimal price, String sku) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.sku = sku;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public Boolean getQuoteOnly() {
        return quoteOnly;
    }

    public void setQuoteOnly(Boolean quoteOnly) {
        this.quoteOnly = quoteOnly;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getOemType() {
        return oemType;
    }

    public void setOemType(String oemType) {
        this.oemType = oemType;
    }

    public String getCompatibilityMode() {
        return compatibilityMode;
    }

    public void setCompatibilityMode(String compatibilityMode) {
        this.compatibilityMode = compatibilityMode;
    }

    public List<String> getCompatibleMakes() {
        return compatibleMakes;
    }

    public void setCompatibleMakes(List<String> compatibleMakes) {
        this.compatibleMakes = compatibleMakes;
    }

    public List<String> getCompatibleModels() {
        return compatibleModels;
    }

    public void setCompatibleModels(List<String> compatibleModels) {
        this.compatibleModels = compatibleModels;
    }

    public Integer getCompatibleYearStart() {
        return compatibleYearStart;
    }

    public void setCompatibleYearStart(Integer compatibleYearStart) {
        this.compatibleYearStart = compatibleYearStart;
    }

    public Integer getCompatibleYearEnd() {
        return compatibleYearEnd;
    }

    public void setCompatibleYearEnd(Integer compatibleYearEnd) {
        this.compatibleYearEnd = compatibleYearEnd;
    }

    public Boolean getVinCompatible() {
        return vinCompatible;
    }

    public void setVinCompatible(Boolean vinCompatible) {
        this.vinCompatible = vinCompatible;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public String getBodyType() {
        return bodyType;
    }

    public void setBodyType(String bodyType) {
        this.bodyType = bodyType;
    }

    public String getDriveType() {
        return driveType;
    }

    public void setDriveType(String driveType) {
        this.driveType = driveType;
    }

    public Integer getPowerKw() {
        return powerKw;
    }

    public void setPowerKw(Integer powerKw) {
        this.powerKw = powerKw;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean getWarrantyIncluded() {
        return warrantyIncluded;
    }

    public void setWarrantyIncluded(Boolean warrantyIncluded) {
        this.warrantyIncluded = warrantyIncluded;
    }

    public String getPartCategory() {
        return partCategory;
    }

    public void setPartCategory(String partCategory) {
        this.partCategory = partCategory;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public List<String> getPartPosition() {
        return partPosition;
    }

    public void setPartPosition(List<String> partPosition) {
        this.partPosition = partPosition;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public Boolean getReconditioned() {
        return reconditioned;
    }

    public void setReconditioned(Boolean reconditioned) {
        this.reconditioned = reconditioned;
    }

    public String getToolCategory() {
        return toolCategory;
    }

    public void setToolCategory(String toolCategory) {
        this.toolCategory = toolCategory;
    }

    public String getPowerSource() {
        return powerSource;
    }

    public void setPowerSource(String powerSource) {
        this.powerSource = powerSource;
    }

    public Integer getVoltage() {
        return voltage;
    }

    public void setVoltage(Integer voltage) {
        this.voltage = voltage;
    }

    public Integer getTorqueMinNm() {
        return torqueMinNm;
    }

    public void setTorqueMinNm(Integer torqueMinNm) {
        this.torqueMinNm = torqueMinNm;
    }

    public Integer getTorqueMaxNm() {
        return torqueMaxNm;
    }

    public void setTorqueMaxNm(Integer torqueMaxNm) {
        this.torqueMaxNm = torqueMaxNm;
    }

    public String getDriveSize() {
        return driveSize;
    }

    public void setDriveSize(String driveSize) {
        this.driveSize = driveSize;
    }

    public Boolean getProfessionalGrade() {
        return professionalGrade;
    }

    public void setProfessionalGrade(Boolean professionalGrade) {
        this.professionalGrade = professionalGrade;
    }

    public Boolean getIsKit() {
        return isKit;
    }

    public void setIsKit(Boolean isKit) {
        this.isKit = isKit;
    }

    public String getCustomCategory() {
        return customCategory;
    }

    public void setCustomCategory(String customCategory) {
        this.customCategory = customCategory;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }

    public void setStyleTags(List<String> styleTags) {
        this.styleTags = styleTags;
    }

    public String getFinish() {
        return finish;
    }

    public void setFinish(String finish) {
        this.finish = finish;
    }

    public Boolean getStreetLegal() {
        return streetLegal;
    }

    public void setStreetLegal(Boolean streetLegal) {
        this.streetLegal = streetLegal;
    }

    public String getInstallationDifficulty() {
        return installationDifficulty;
    }

    public void setInstallationDifficulty(String installationDifficulty) {
        this.installationDifficulty = installationDifficulty;
    }

    public String getInfoSection1Title() {
        return infoSection1Title;
    }

    public void setInfoSection1Title(String infoSection1Title) {
        this.infoSection1Title = infoSection1Title;
    }

    public String getInfoSection1Content() {
        return infoSection1Content;
    }

    public void setInfoSection1Content(String infoSection1Content) {
        this.infoSection1Content = infoSection1Content;
    }

    public Boolean getInfoSection1Enabled() {
        return infoSection1Enabled;
    }

    public void setInfoSection1Enabled(Boolean infoSection1Enabled) {
        this.infoSection1Enabled = infoSection1Enabled;
    }

    public String getInfoSection2Title() {
        return infoSection2Title;
    }

    public void setInfoSection2Title(String infoSection2Title) {
        this.infoSection2Title = infoSection2Title;
    }

    public String getInfoSection2Content() {
        return infoSection2Content;
    }

    public void setInfoSection2Content(String infoSection2Content) {
        this.infoSection2Content = infoSection2Content;
    }

    public Boolean getInfoSection2Enabled() {
        return infoSection2Enabled;
    }

    public void setInfoSection2Enabled(Boolean infoSection2Enabled) {
        this.infoSection2Enabled = infoSection2Enabled;
    }

    public String getInfoSection3Title() {
        return infoSection3Title;
    }

    public void setInfoSection3Title(String infoSection3Title) {
        this.infoSection3Title = infoSection3Title;
    }

    public String getInfoSection3Content() {
        return infoSection3Content;
    }

    public void setInfoSection3Content(String infoSection3Content) {
        this.infoSection3Content = infoSection3Content;
    }

    public Boolean getInfoSection3Enabled() {
        return infoSection3Enabled;
    }

    public void setInfoSection3Enabled(Boolean infoSection3Enabled) {
        this.infoSection3Enabled = infoSection3Enabled;
    }

    public String getInfoSection4Title() {
        return infoSection4Title;
    }

    public void setInfoSection4Title(String infoSection4Title) {
        this.infoSection4Title = infoSection4Title;
    }

    public String getInfoSection4Content() {
        return infoSection4Content;
    }

    public void setInfoSection4Content(String infoSection4Content) {
        this.infoSection4Content = infoSection4Content;
    }

    public Boolean getInfoSection4Enabled() {
        return infoSection4Enabled;
    }

    public void setInfoSection4Enabled(Boolean infoSection4Enabled) {
        this.infoSection4Enabled = infoSection4Enabled;
    }

    public String getInfoSection5Title() {
        return infoSection5Title;
    }

    public void setInfoSection5Title(String infoSection5Title) {
        this.infoSection5Title = infoSection5Title;
    }

    public String getInfoSection5Content() {
        return infoSection5Content;
    }

    public void setInfoSection5Content(String infoSection5Content) {
        this.infoSection5Content = infoSection5Content;
    }

    public Boolean getInfoSection5Enabled() {
        return infoSection5Enabled;
    }

    public void setInfoSection5Enabled(Boolean infoSection5Enabled) {
        this.infoSection5Enabled = infoSection5Enabled;
    }

    public String getInfoSection6Title() {
        return infoSection6Title;
    }

    public void setInfoSection6Title(String infoSection6Title) {
        this.infoSection6Title = infoSection6Title;
    }

    public String getInfoSection6Content() {
        return infoSection6Content;
    }

    public void setInfoSection6Content(String infoSection6Content) {
        this.infoSection6Content = infoSection6Content;
    }

    public Boolean getInfoSection6Enabled() {
        return infoSection6Enabled;
    }

    public void setInfoSection6Enabled(Boolean infoSection6Enabled) {
        this.infoSection6Enabled = infoSection6Enabled;
    }

    public String getInfoSection7Title() {
        return infoSection7Title;
    }

    public void setInfoSection7Title(String infoSection7Title) {
        this.infoSection7Title = infoSection7Title;
    }

    public String getInfoSection7Content() {
        return infoSection7Content;
    }

    public void setInfoSection7Content(String infoSection7Content) {
        this.infoSection7Content = infoSection7Content;
    }

    public Boolean getInfoSection7Enabled() {
        return infoSection7Enabled;
    }

    public void setInfoSection7Enabled(Boolean infoSection7Enabled) {
        this.infoSection7Enabled = infoSection7Enabled;
    }

    public String getInfoSection8Title() {
        return infoSection8Title;
    }

    public void setInfoSection8Title(String infoSection8Title) {
        this.infoSection8Title = infoSection8Title;
    }

    public String getInfoSection8Content() {
        return infoSection8Content;
    }

    public void setInfoSection8Content(String infoSection8Content) {
        this.infoSection8Content = infoSection8Content;
    }

    public Boolean getInfoSection8Enabled() {
        return infoSection8Enabled;
    }

    public void setInfoSection8Enabled(Boolean infoSection8Enabled) {
        this.infoSection8Enabled = infoSection8Enabled;
    }

    public String getInfoSection9Title() {
        return infoSection9Title;
    }

    public void setInfoSection9Title(String infoSection9Title) {
        this.infoSection9Title = infoSection9Title;
    }

    public String getInfoSection9Content() {
        return infoSection9Content;
    }

    public void setInfoSection9Content(String infoSection9Content) {
        this.infoSection9Content = infoSection9Content;
    }

    public Boolean getInfoSection9Enabled() {
        return infoSection9Enabled;
    }

    public void setInfoSection9Enabled(Boolean infoSection9Enabled) {
        this.infoSection9Enabled = infoSection9Enabled;
    }

    public String getInfoSection10Title() {
        return infoSection10Title;
    }

    public void setInfoSection10Title(String infoSection10Title) {
        this.infoSection10Title = infoSection10Title;
    }

    public String getInfoSection10Content() {
        return infoSection10Content;
    }

    public void setInfoSection10Content(String infoSection10Content) {
        this.infoSection10Content = infoSection10Content;
    }

    public Boolean getInfoSection10Enabled() {
        return infoSection10Enabled;
    }

    public void setInfoSection10Enabled(Boolean infoSection10Enabled) {
        this.infoSection10Enabled = infoSection10Enabled;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Boolean getInStock() {
        return inStock;
    }

    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public java.util.List<ProductImageResponse> getImages() {
        return images;
    }

    public void setImages(java.util.List<ProductImageResponse> images) {
        this.images = images;
    }

    public java.util.List<ProductVariantResponse> getVariants() {
        return variants;
    }

    public void setVariants(java.util.List<ProductVariantResponse> variants) {
        this.variants = variants;
    }
}
