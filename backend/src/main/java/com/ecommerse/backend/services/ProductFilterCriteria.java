package com.ecommerse.backend.services;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregate filter criteria for product catalog queries.
 */
public class ProductFilterCriteria {
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String brand;
    private Boolean inStockOnly;
    private String condition;
    private String productType;

    private String make;
    private String model;
    private Integer yearMin;
    private Integer yearMax;
    private Integer mileageMin;
    private Integer mileageMax;
    private String fuelType;
    private String transmission;
    private String bodyType;
    private String driveType;
    private Integer powerMin;
    private Integer powerMax;
    private Boolean warrantyIncluded;

    private String compatibilityMode;
    private String compatibleMake;
    private String compatibleModel;
    private Integer compatibleYear;
    private String oemType;
    private String partCategory;
    private String partsMainCategory;
    private String partsSubCategory;
    private String partsDeepCategory;
    private String partNumber;
    private List<String> partPosition;

    private BigDecimal wheelDiameterMin;
    private BigDecimal wheelDiameterMax;
    private BigDecimal wheelWidthMin;
    private BigDecimal wheelWidthMax;
    private Integer wheelOffsetMin;
    private Integer wheelOffsetMax;
    private BigDecimal centerBoreMin;
    private BigDecimal centerBoreMax;
    private String wheelBoltPattern;
    private String wheelMaterial;
    private String wheelColor;
    private Boolean hubCentricRingsNeeded;

    private String engineType;
    private Integer engineDisplacementMin;
    private Integer engineDisplacementMax;
    private Integer engineCylinders;
    private Integer enginePowerMin;
    private Integer enginePowerMax;
    private String turboType;
    private String flangeType;
    private String wastegateType;

    private Integer rotorDiameterMin;
    private Integer rotorDiameterMax;
    private String padCompound;
    private Boolean adjustableHeight;
    private Boolean adjustableDamping;
    private String lightingVoltage;
    private String bulbType;

    private String toolCategory;
    private String powerSource;
    private Integer voltageMin;
    private Integer voltageMax;
    private Integer torqueMin;
    private Integer torqueMax;
    private String driveSize;
    private Boolean professionalGrade;
    private Boolean isKit;

    private List<String> styleTags;
    private String finish;
    private Boolean streetLegal;
    private String installationDifficulty;
    private String customCategory;

    public boolean hasAdvancedFilters() {
        return minPrice != null
                || maxPrice != null
                || hasText(brand)
                || Boolean.TRUE.equals(inStockOnly)
                || hasText(condition)
                || hasText(productType)
                || hasText(make)
                || hasText(model)
                || yearMin != null
                || yearMax != null
                || mileageMin != null
                || mileageMax != null
                || hasText(fuelType)
                || hasText(transmission)
                || hasText(bodyType)
                || hasText(driveType)
                || powerMin != null
                || powerMax != null
                || warrantyIncluded != null
                || hasText(compatibilityMode)
                || hasText(compatibleMake)
                || hasText(compatibleModel)
                || compatibleYear != null
                || hasText(oemType)
                || hasText(partCategory)
                || hasText(partsMainCategory)
                || hasText(partsSubCategory)
                || hasText(partsDeepCategory)
                || hasText(partNumber)
                || (partPosition != null && !partPosition.isEmpty())
                || wheelDiameterMin != null
                || wheelDiameterMax != null
                || wheelWidthMin != null
                || wheelWidthMax != null
                || wheelOffsetMin != null
                || wheelOffsetMax != null
                || centerBoreMin != null
                || centerBoreMax != null
                || hasText(wheelBoltPattern)
                || hasText(wheelMaterial)
                || hasText(wheelColor)
                || hubCentricRingsNeeded != null
                || hasText(engineType)
                || engineDisplacementMin != null
                || engineDisplacementMax != null
                || engineCylinders != null
                || enginePowerMin != null
                || enginePowerMax != null
                || hasText(turboType)
                || hasText(flangeType)
                || hasText(wastegateType)
                || rotorDiameterMin != null
                || rotorDiameterMax != null
                || hasText(padCompound)
                || adjustableHeight != null
                || adjustableDamping != null
                || hasText(lightingVoltage)
                || hasText(bulbType)
                || hasText(toolCategory)
                || hasText(powerSource)
                || voltageMin != null
                || voltageMax != null
                || torqueMin != null
                || torqueMax != null
                || hasText(driveSize)
                || professionalGrade != null
                || isKit != null
                || (styleTags != null && !styleTags.isEmpty())
                || hasText(finish)
                || streetLegal != null
                || hasText(installationDifficulty)
                || hasText(customCategory);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Boolean getInStockOnly() {
        return inStockOnly;
    }

    public void setInStockOnly(Boolean inStockOnly) {
        this.inStockOnly = inStockOnly;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
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

    public Integer getYearMin() {
        return yearMin;
    }

    public void setYearMin(Integer yearMin) {
        this.yearMin = yearMin;
    }

    public Integer getYearMax() {
        return yearMax;
    }

    public void setYearMax(Integer yearMax) {
        this.yearMax = yearMax;
    }

    public Integer getMileageMin() {
        return mileageMin;
    }

    public void setMileageMin(Integer mileageMin) {
        this.mileageMin = mileageMin;
    }

    public Integer getMileageMax() {
        return mileageMax;
    }

    public void setMileageMax(Integer mileageMax) {
        this.mileageMax = mileageMax;
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

    public Integer getPowerMin() {
        return powerMin;
    }

    public void setPowerMin(Integer powerMin) {
        this.powerMin = powerMin;
    }

    public Integer getPowerMax() {
        return powerMax;
    }

    public void setPowerMax(Integer powerMax) {
        this.powerMax = powerMax;
    }

    public Boolean getWarrantyIncluded() {
        return warrantyIncluded;
    }

    public void setWarrantyIncluded(Boolean warrantyIncluded) {
        this.warrantyIncluded = warrantyIncluded;
    }

    public String getCompatibilityMode() {
        return compatibilityMode;
    }

    public void setCompatibilityMode(String compatibilityMode) {
        this.compatibilityMode = compatibilityMode;
    }

    public String getCompatibleMake() {
        return compatibleMake;
    }

    public void setCompatibleMake(String compatibleMake) {
        this.compatibleMake = compatibleMake;
    }

    public String getCompatibleModel() {
        return compatibleModel;
    }

    public void setCompatibleModel(String compatibleModel) {
        this.compatibleModel = compatibleModel;
    }

    public Integer getCompatibleYear() {
        return compatibleYear;
    }

    public void setCompatibleYear(Integer compatibleYear) {
        this.compatibleYear = compatibleYear;
    }

    public String getOemType() {
        return oemType;
    }

    public void setOemType(String oemType) {
        this.oemType = oemType;
    }

    public String getPartCategory() {
        return partCategory;
    }

    public void setPartCategory(String partCategory) {
        this.partCategory = partCategory;
    }

    public String getPartsMainCategory() {
        return partsMainCategory;
    }

    public void setPartsMainCategory(String partsMainCategory) {
        this.partsMainCategory = partsMainCategory;
    }

    public String getPartsSubCategory() {
        return partsSubCategory;
    }

    public void setPartsSubCategory(String partsSubCategory) {
        this.partsSubCategory = partsSubCategory;
    }

    public String getPartsDeepCategory() {
        return partsDeepCategory;
    }

    public void setPartsDeepCategory(String partsDeepCategory) {
        this.partsDeepCategory = partsDeepCategory;
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

    public BigDecimal getWheelDiameterMin() {
        return wheelDiameterMin;
    }

    public void setWheelDiameterMin(BigDecimal wheelDiameterMin) {
        this.wheelDiameterMin = wheelDiameterMin;
    }

    public BigDecimal getWheelDiameterMax() {
        return wheelDiameterMax;
    }

    public void setWheelDiameterMax(BigDecimal wheelDiameterMax) {
        this.wheelDiameterMax = wheelDiameterMax;
    }

    public BigDecimal getWheelWidthMin() {
        return wheelWidthMin;
    }

    public void setWheelWidthMin(BigDecimal wheelWidthMin) {
        this.wheelWidthMin = wheelWidthMin;
    }

    public BigDecimal getWheelWidthMax() {
        return wheelWidthMax;
    }

    public void setWheelWidthMax(BigDecimal wheelWidthMax) {
        this.wheelWidthMax = wheelWidthMax;
    }

    public Integer getWheelOffsetMin() {
        return wheelOffsetMin;
    }

    public void setWheelOffsetMin(Integer wheelOffsetMin) {
        this.wheelOffsetMin = wheelOffsetMin;
    }

    public Integer getWheelOffsetMax() {
        return wheelOffsetMax;
    }

    public void setWheelOffsetMax(Integer wheelOffsetMax) {
        this.wheelOffsetMax = wheelOffsetMax;
    }

    public BigDecimal getCenterBoreMin() {
        return centerBoreMin;
    }

    public void setCenterBoreMin(BigDecimal centerBoreMin) {
        this.centerBoreMin = centerBoreMin;
    }

    public BigDecimal getCenterBoreMax() {
        return centerBoreMax;
    }

    public void setCenterBoreMax(BigDecimal centerBoreMax) {
        this.centerBoreMax = centerBoreMax;
    }

    public String getWheelBoltPattern() {
        return wheelBoltPattern;
    }

    public void setWheelBoltPattern(String wheelBoltPattern) {
        this.wheelBoltPattern = wheelBoltPattern;
    }

    public String getWheelMaterial() {
        return wheelMaterial;
    }

    public void setWheelMaterial(String wheelMaterial) {
        this.wheelMaterial = wheelMaterial;
    }

    public String getWheelColor() {
        return wheelColor;
    }

    public void setWheelColor(String wheelColor) {
        this.wheelColor = wheelColor;
    }

    public Boolean getHubCentricRingsNeeded() {
        return hubCentricRingsNeeded;
    }

    public void setHubCentricRingsNeeded(Boolean hubCentricRingsNeeded) {
        this.hubCentricRingsNeeded = hubCentricRingsNeeded;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public Integer getEngineDisplacementMin() {
        return engineDisplacementMin;
    }

    public void setEngineDisplacementMin(Integer engineDisplacementMin) {
        this.engineDisplacementMin = engineDisplacementMin;
    }

    public Integer getEngineDisplacementMax() {
        return engineDisplacementMax;
    }

    public void setEngineDisplacementMax(Integer engineDisplacementMax) {
        this.engineDisplacementMax = engineDisplacementMax;
    }

    public Integer getEngineCylinders() {
        return engineCylinders;
    }

    public void setEngineCylinders(Integer engineCylinders) {
        this.engineCylinders = engineCylinders;
    }

    public Integer getEnginePowerMin() {
        return enginePowerMin;
    }

    public void setEnginePowerMin(Integer enginePowerMin) {
        this.enginePowerMin = enginePowerMin;
    }

    public Integer getEnginePowerMax() {
        return enginePowerMax;
    }

    public void setEnginePowerMax(Integer enginePowerMax) {
        this.enginePowerMax = enginePowerMax;
    }

    public String getTurboType() {
        return turboType;
    }

    public void setTurboType(String turboType) {
        this.turboType = turboType;
    }

    public String getFlangeType() {
        return flangeType;
    }

    public void setFlangeType(String flangeType) {
        this.flangeType = flangeType;
    }

    public String getWastegateType() {
        return wastegateType;
    }

    public void setWastegateType(String wastegateType) {
        this.wastegateType = wastegateType;
    }

    public Integer getRotorDiameterMin() {
        return rotorDiameterMin;
    }

    public void setRotorDiameterMin(Integer rotorDiameterMin) {
        this.rotorDiameterMin = rotorDiameterMin;
    }

    public Integer getRotorDiameterMax() {
        return rotorDiameterMax;
    }

    public void setRotorDiameterMax(Integer rotorDiameterMax) {
        this.rotorDiameterMax = rotorDiameterMax;
    }

    public String getPadCompound() {
        return padCompound;
    }

    public void setPadCompound(String padCompound) {
        this.padCompound = padCompound;
    }

    public Boolean getAdjustableHeight() {
        return adjustableHeight;
    }

    public void setAdjustableHeight(Boolean adjustableHeight) {
        this.adjustableHeight = adjustableHeight;
    }

    public Boolean getAdjustableDamping() {
        return adjustableDamping;
    }

    public void setAdjustableDamping(Boolean adjustableDamping) {
        this.adjustableDamping = adjustableDamping;
    }

    public String getLightingVoltage() {
        return lightingVoltage;
    }

    public void setLightingVoltage(String lightingVoltage) {
        this.lightingVoltage = lightingVoltage;
    }

    public String getBulbType() {
        return bulbType;
    }

    public void setBulbType(String bulbType) {
        this.bulbType = bulbType;
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

    public Integer getVoltageMin() {
        return voltageMin;
    }

    public void setVoltageMin(Integer voltageMin) {
        this.voltageMin = voltageMin;
    }

    public Integer getVoltageMax() {
        return voltageMax;
    }

    public void setVoltageMax(Integer voltageMax) {
        this.voltageMax = voltageMax;
    }

    public Integer getTorqueMin() {
        return torqueMin;
    }

    public void setTorqueMin(Integer torqueMin) {
        this.torqueMin = torqueMin;
    }

    public Integer getTorqueMax() {
        return torqueMax;
    }

    public void setTorqueMax(Integer torqueMax) {
        this.torqueMax = torqueMax;
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

    public String getCustomCategory() {
        return customCategory;
    }

    public void setCustomCategory(String customCategory) {
        this.customCategory = customCategory;
    }
}
