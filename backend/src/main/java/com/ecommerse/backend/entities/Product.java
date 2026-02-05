package com.ecommerse.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Product entity representing items in the e-commerce system
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(length = 1000)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be at least 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @NotBlank(message = "SKU is required")
    @Size(min = 3, max = 50, message = "SKU must be between 3 and 50 characters")
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(name = "quote_only", nullable = false)
    private Boolean quoteOnly = false;

    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    @Digits(integer = 5, fraction = 2, message = "Weight must have at most 5 integer digits and 2 decimal places")
    @Column(precision = 7, scale = 2)
    private BigDecimal weight;

    @Size(max = 100, message = "Brand name cannot exceed 100 characters")
    @Column(length = 100)
    private String brand;

    @Column(name = "product_type", length = 20)
    private String productType;

    @Column(name = "product_condition", length = 20)
    private String condition;

    @Column(name = "oem_type", length = 20)
    private String oemType;

    @Column(name = "compatibility_mode", length = 20)
    private String compatibilityMode;

    @Column(name = "compatible_makes", length = 2000)
    private String compatibleMakes;

    @Column(name = "compatible_models", length = 2000)
    private String compatibleModels;

    @Column(name = "compatible_year_start")
    private Integer compatibleYearStart;

    @Column(name = "compatible_year_end")
    private Integer compatibleYearEnd;

    @Column(name = "vin_compatible")
    private Boolean vinCompatible;

    @Column(length = 100)
    private String make;

    @Column(length = 100)
    private String model;

    @Column(name = "car_year")
    private Integer year;

    @Column
    private Integer mileage;

    @Column(name = "fuel_type", length = 40)
    private String fuelType;

    @Column(name = "transmission", length = 40)
    private String transmission;

    @Column(name = "body_type", length = 40)
    private String bodyType;

    @Column(name = "drive_type", length = 40)
    private String driveType;

    @Column(name = "power_kw")
    private Integer powerKw;

    @Column(length = 100)
    private String color;

    @Column(name = "warranty_included")
    private Boolean warrantyIncluded;

    @Column(name = "part_category", length = 120)
    private String partCategory;

    @Column(name = "part_number", length = 120)
    private String partNumber;

    @Column(name = "part_position", length = 500)
    private String partPosition;

    @Column(length = 120)
    private String material;

    @Column
    private Boolean reconditioned;

    @Column(name = "tool_category", length = 120)
    private String toolCategory;

    @Column(name = "power_source", length = 40)
    private String powerSource;

    @Column
    private Integer voltage;

    @Column(name = "torque_min_nm")
    private Integer torqueMinNm;

    @Column(name = "torque_max_nm")
    private Integer torqueMaxNm;

    @Column(name = "drive_size", length = 40)
    private String driveSize;

    @Column(name = "professional_grade")
    private Boolean professionalGrade;

    @Column(name = "is_kit")
    private Boolean isKit;

    @Column(name = "custom_category", length = 120)
    private String customCategory;

    @Column(name = "style_tags", length = 1000)
    private String styleTags;

    @Column(name = "finish", length = 80)
    private String finish;

    @Column(name = "street_legal")
    private Boolean streetLegal;

    @Column(name = "installation_difficulty", length = 40)
    private String installationDifficulty;

    @Column(name = "info_section1_title", length = 120)
    private String infoSection1Title;

    @Column(name = "info_section1_content", length = 2000)
    private String infoSection1Content;

    @Column(name = "info_section1_enabled")
    private Boolean infoSection1Enabled = Boolean.FALSE;

    @Column(name = "info_section2_title", length = 120)
    private String infoSection2Title;

    @Column(name = "info_section2_content", length = 2000)
    private String infoSection2Content;

    @Column(name = "info_section2_enabled")
    private Boolean infoSection2Enabled = Boolean.FALSE;

    @Column(name = "info_section3_title", length = 120)
    private String infoSection3Title;

    @Column(name = "info_section3_content", length = 2000)
    private String infoSection3Content;

    @Column(name = "info_section3_enabled")
    private Boolean infoSection3Enabled = Boolean.FALSE;

    @Column(name = "info_section4_title", length = 120)
    private String infoSection4Title;

    @Column(name = "info_section4_content", length = 2000)
    private String infoSection4Content;

    @Column(name = "info_section4_enabled")
    private Boolean infoSection4Enabled = Boolean.FALSE;

    @Column(name = "info_section5_title", length = 120)
    private String infoSection5Title;

    @Column(name = "info_section5_content", length = 2000)
    private String infoSection5Content;

    @Column(name = "info_section5_enabled")
    private Boolean infoSection5Enabled = Boolean.FALSE;

    @Column(name = "info_section6_title", length = 120)
    private String infoSection6Title;

    @Column(name = "info_section6_content", length = 2000)
    private String infoSection6Content;

    @Column(name = "info_section6_enabled")
    private Boolean infoSection6Enabled = Boolean.FALSE;

    @Column(name = "info_section7_title", length = 120)
    private String infoSection7Title;

    @Column(name = "info_section7_content", length = 2000)
    private String infoSection7Content;

    @Column(name = "info_section7_enabled")
    private Boolean infoSection7Enabled = Boolean.FALSE;

    @Column(name = "info_section8_title", length = 120)
    private String infoSection8Title;

    @Column(name = "info_section8_content", length = 2000)
    private String infoSection8Content;

    @Column(name = "info_section8_enabled")
    private Boolean infoSection8Enabled = Boolean.FALSE;

    @Column(name = "info_section9_title", length = 120)
    private String infoSection9Title;

    @Column(name = "info_section9_content", length = 2000)
    private String infoSection9Content;

    @Column(name = "info_section9_enabled")
    private Boolean infoSection9Enabled = Boolean.FALSE;

    @Column(name = "info_section10_title", length = 120)
    private String infoSection10Title;

    @Column(name = "info_section10_content", length = 2000)
    private String infoSection10Content;

    @Column(name = "info_section10_enabled")
    private Boolean infoSection10Enabled = Boolean.FALSE;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @NotNull(message = "Category is required")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<CartItem> cartItems = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @BatchSize(size = 25)
    @Fetch(FetchMode.SUBSELECT)
    @JsonIgnore
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @BatchSize(size = 25)
    @Fetch(FetchMode.SUBSELECT)
    @JsonIgnore
    private Set<ProductVariant> variants = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    // Constructors
    public Product() {
    }

    public Product(String name, String description, BigDecimal price, String sku, Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.sku = sku;
        this.category = category;
    }

    // Business methods
    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public boolean isAvailable() {
        return active && isInStock();
    }

    public void reduceStock(int quantity) {
        if (stockQuantity < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available: " + stockQuantity + ", Requested: " + quantity);
        }
        this.stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.stockQuantity += quantity;
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

    public String getCompatibleMakes() {
        return compatibleMakes;
    }

    public void setCompatibleMakes(String compatibleMakes) {
        this.compatibleMakes = compatibleMakes;
    }

    public String getCompatibleModels() {
        return compatibleModels;
    }

    public void setCompatibleModels(String compatibleModels) {
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

    public String getPartPosition() {
        return partPosition;
    }

    public void setPartPosition(String partPosition) {
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

    public String getStyleTags() {
        return styleTags;
    }

    public void setStyleTags(String styleTags) {
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    public Set<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(Iterable<ProductVariant> variants) {
        this.variants.clear();
        if (variants != null) {
            variants.forEach(this::addVariant);
        }
    }

    public void addVariant(ProductVariant variant) {
        if (variant == null) {
            return;
        }
        variant.setProduct(this);
        this.variants.add(variant);
    }

    public void removeVariant(ProductVariant variant) {
        if (variant == null) {
            return;
        }
        variant.setProduct(null);
        this.variants.remove(variant);
    }

    // Image helper methods
    public ProductImage getMainImage() {
        return images.stream()
                .filter(ProductImage::getIsMain)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0));
    }

    public String getMainImageUrl() {
        ProductImage mainImage = getMainImage();
        return mainImage != null ? mainImage.getImageUrl() : this.imageUrl;
    }

    public void addImage(ProductImage image) {
        image.setProduct(this);
        images.add(image);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Product))
            return false;
        Product product = (Product) o;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", sku='" + sku + '\'' +
                ", active=" + active +
                '}';
    }
}
