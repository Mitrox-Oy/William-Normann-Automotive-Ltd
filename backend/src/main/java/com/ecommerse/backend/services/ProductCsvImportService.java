package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductCsvImportResult;
import com.ecommerse.backend.dto.ProductCsvImportRowResult;
import com.ecommerse.backend.dto.ProductDTO;
import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.repositories.CategoryRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductCsvImportService {

    private static final int MAX_IMPORT_ROWS = 5000;

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public ProductCsvImportService(
            ProductService productService,
            CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.productService = productService;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductCsvImportResult importCsv(MultipartFile file, boolean dryRun) throws IOException {
        validateFile(file);

        ProductCsvImportResult result = new ProductCsvImportResult();
        result.setDryRun(dryRun);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);
            validateHeaderStructure(headerIndex);

            String line;
            int csvRowNumber = 1;
            int dataRowCount = 0;

            while ((line = reader.readLine()) != null) {
                csvRowNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                dataRowCount++;
                if (dataRowCount > MAX_IMPORT_ROWS) {
                    throw new IllegalArgumentException(
                            "CSV row limit exceeded. Max rows per import: " + MAX_IMPORT_ROWS);
                }

                ProductCsvImportRowResult rowResult = processRow(csvRowNumber, line, headerIndex, dryRun);
                result.getRows().add(rowResult);
            }
        }

        result.setTotalRows(result.getRows().size());
        int successCount = (int) result.getRows().stream().filter(row -> row.getErrors().isEmpty()).count();
        result.setSuccessCount(successCount);
        result.setFailedCount(result.getTotalRows() - successCount);

        return result;
    }

    private ProductCsvImportRowResult processRow(
            int csvRowNumber,
            String line,
            Map<String, Integer> headerIndex,
            boolean dryRun) {

        ProductCsvImportRowResult rowResult = new ProductCsvImportRowResult();
        rowResult.setRowNumber(csvRowNumber);

        List<String> values = parseCsvLine(line);

        try {
            ProductDTO dto = mapRowToProduct(values, headerIndex, rowResult);
            rowResult.setName(dto.getName());

            if (!rowResult.getErrors().isEmpty()) {
                rowResult.setStatus("failed");
                return rowResult;
            }

            if (dryRun) {
                rowResult.setStatus("validated");
                rowResult.setSku(dto.getSku());
                return rowResult;
            }

            ProductDTO created = productService.createProduct(dto);
            rowResult.setStatus("imported");
            rowResult.setProductId(created.getId());
            rowResult.setSku(created.getSku());
            return rowResult;
        } catch (IllegalArgumentException ex) {
            rowResult.getErrors().add(ex.getMessage());
            rowResult.setStatus("failed");
            return rowResult;
        } catch (Exception ex) {
            rowResult.getErrors().add("Unexpected error: " + ex.getMessage());
            rowResult.setStatus("failed");
            return rowResult;
        }
    }

    private ProductDTO mapRowToProduct(
            List<String> values,
            Map<String, Integer> headerIndex,
            ProductCsvImportRowResult rowResult) {

        ProductDTO dto = new ProductDTO();

        String name = getValue(values, headerIndex, "name");
        if (name == null || name.isBlank()) {
            rowResult.getErrors().add("name is required");
        } else {
            dto.setName(name);
        }

        BigDecimal price = parseBigDecimal(getValue(values, headerIndex, "price"), "price", rowResult);
        dto.setPrice(price);

        Long categoryId = resolveCategoryId(values, headerIndex, rowResult);
        dto.setCategoryId(categoryId);

        dto.setDescription(getValue(values, headerIndex, "description"));
        dto.setSku(emptyToNull(getValue(values, headerIndex, "sku")));
        dto.setImageUrl(getValue(values, headerIndex, "imageUrl"));
        dto.setBrand(getValue(values, headerIndex, "brand"));
        dto.setCondition(getValue(values, headerIndex, "condition"));
        dto.setProductType(getValue(values, headerIndex, "productType"));
        dto.setMake(getValue(values, headerIndex, "make"));
        dto.setModel(getValue(values, headerIndex, "model"));
        dto.setFuelType(getValue(values, headerIndex, "fuelType"));
        dto.setTransmission(getValue(values, headerIndex, "transmission"));
        dto.setBodyType(getValue(values, headerIndex, "bodyType"));
        dto.setDriveType(getValue(values, headerIndex, "driveType"));
        dto.setColor(getValue(values, headerIndex, "color"));
        dto.setPartCategory(getValue(values, headerIndex, "partCategory"));
        dto.setOemType(getValue(values, headerIndex, "oemType"));
        dto.setPartNumber(getValue(values, headerIndex, "partNumber"));
        dto.setToolCategory(getValue(values, headerIndex, "toolCategory"));
        dto.setPowerSource(getValue(values, headerIndex, "powerSource"));
        dto.setCustomCategory(getValue(values, headerIndex, "customCategory"));
        dto.setFinish(getValue(values, headerIndex, "finish"));
        dto.setInstallationDifficulty(getValue(values, headerIndex, "installationDifficulty"));

        dto.setStockQuantity(parseInteger(getValue(values, headerIndex, "stockQuantity"), "stockQuantity", rowResult));
        dto.setYear(parseInteger(getValue(values, headerIndex, "year"), "year", rowResult));
        dto.setMileage(parseInteger(getValue(values, headerIndex, "mileage"), "mileage", rowResult));
        dto.setPowerKw(parseInteger(getValue(values, headerIndex, "powerKw"), "powerKw", rowResult));

        dto.setActive(parseBoolean(getValue(values, headerIndex, "active"), "active", rowResult));
        dto.setFeatured(parseBoolean(getValue(values, headerIndex, "featured"), "featured", rowResult));
        dto.setQuoteOnly(parseBoolean(getValue(values, headerIndex, "quoteOnly"), "quoteOnly", rowResult));
        dto.setStreetLegal(parseBoolean(getValue(values, headerIndex, "streetLegal"), "streetLegal", rowResult));
        dto.setWarrantyIncluded(
                parseBoolean(getValue(values, headerIndex, "warrantyIncluded"), "warrantyIncluded", rowResult));

        BigDecimal weight = parseBigDecimal(getValue(values, headerIndex, "weight"), "weight", rowResult);
        dto.setWeight(weight);

        List<String> styleTags = parseList(getValue(values, headerIndex, "styleTags"));
        if (!styleTags.isEmpty()) {
            dto.setStyleTags(styleTags);
        }

        List<String> partPosition = parseList(getValue(values, headerIndex, "partPosition"));
        if (!partPosition.isEmpty()) {
            dto.setPartPosition(partPosition);
        }

        if (dto.getSku() != null && productRepository.existsBySku(dto.getSku())) {
            rowResult.getErrors().add("SKU already exists: " + dto.getSku());
        }

        if (dto.getPrice() == null) {
            rowResult.getErrors().add("price is required");
        } else if (dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            rowResult.getErrors().add("price must be greater than 0");
        }

        return dto;
    }

    private Long resolveCategoryId(List<String> values, Map<String, Integer> headerIndex,
            ProductCsvImportRowResult rowResult) {
        String categoryIdRaw = getValue(values, headerIndex, "categoryId");
        if (categoryIdRaw != null && !categoryIdRaw.isBlank()) {
            try {
                Long categoryId = Long.parseLong(categoryIdRaw.trim());
                Optional<Category> category = categoryRepository.findByIdAndActiveTrue(categoryId);
                if (category.isEmpty()) {
                    rowResult.getErrors().add("categoryId not found: " + categoryIdRaw);
                    return null;
                }
                return categoryId;
            } catch (NumberFormatException ex) {
                rowResult.getErrors().add("Invalid categoryId: " + categoryIdRaw);
                return null;
            }
        }

        String categorySlug = getValue(values, headerIndex, "categorySlug");
        if (categorySlug == null || categorySlug.isBlank()) {
            rowResult.getErrors().add("Either categoryId or categorySlug is required");
            return null;
        }

        Optional<Category> category = categoryRepository.findBySlugIgnoreCaseAndActiveTrue(categorySlug.trim());
        if (category.isEmpty()) {
            rowResult.getErrors().add("categorySlug not found: " + categorySlug);
            return null;
        }

        return category.get().getId();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are supported");
        }
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = normalizeHeader(headers.get(i));
            if (!key.isBlank()) {
                index.putIfAbsent(key, i);
            }
        }
        return index;
    }

    private void validateHeaderStructure(Map<String, Integer> headerIndex) {
        if (!headerIndex.containsKey("name")) {
            throw new IllegalArgumentException("CSV header must include 'name'");
        }
        if (!headerIndex.containsKey("price")) {
            throw new IllegalArgumentException("CSV header must include 'price'");
        }
        if (!headerIndex.containsKey("categoryid") && !headerIndex.containsKey("categoryslug")) {
            throw new IllegalArgumentException("CSV header must include 'categoryId' or 'categorySlug'");
        }
    }

    private String normalizeHeader(String value) {
        if (value == null)
            return "";
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String getValue(List<String> values, Map<String, Integer> headerIndex, String fieldName) {
        Integer index = headerIndex.get(normalizeHeader(fieldName));
        if (index == null || index < 0 || index >= values.size()) {
            return null;
        }
        String value = values.get(index);
        return value == null ? null : value.trim();
    }

    private Integer parseInteger(String raw, String fieldName, ProductCsvImportRowResult rowResult) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            rowResult.getErrors().add("Invalid integer for " + fieldName + ": " + raw);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String raw, String fieldName, ProductCsvImportRowResult rowResult) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            rowResult.getErrors().add("Invalid decimal for " + fieldName + ": " + raw);
            return null;
        }
    }

    private Boolean parseBoolean(String raw, String fieldName, ProductCsvImportRowResult rowResult) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("true") || normalized.equals("1") || normalized.equals("yes")) {
            return true;
        }
        if (normalized.equals("false") || normalized.equals("0") || normalized.equals("no")) {
            return false;
        }

        rowResult.getErrors().add("Invalid boolean for " + fieldName + ": " + raw);
        return null;
    }

    private List<String> parseList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }

        return java.util.Arrays.stream(raw.split("[|;,]"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .toList();
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        values.add(current.toString());
        return values;
    }
}
