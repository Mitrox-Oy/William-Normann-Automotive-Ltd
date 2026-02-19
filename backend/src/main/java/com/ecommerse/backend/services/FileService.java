package com.ecommerse.backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling file uploads and management
 */
@Service
public class FileService {

    private final UploadedImageService uploadedImageService;

    public FileService(UploadedImageService uploadedImageService) {
        this.uploadedImageService = uploadedImageService;
    }

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${app.upload.public-url-prefix:/uploads}")
    private String publicUrlPrefix;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp");

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    /**
     * Upload a product image
     */
    public String uploadProductImage(MultipartFile file) throws IOException {
        validateFile(file);

        // Generate unique filename
        String fileName = generateFileName(file);

        // Persist to DB first so image survives dyno/container restarts.
        uploadedImageService.save(fileName, file);

        // Keep filesystem copy as a best-effort cache for direct static serving.
        try {
            Path uploadPath = createUploadDirectory("products");
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("File-system cache write failed for product image " + fileName + ": " + e.getMessage());
        }

        // Return relative path
        return "products/" + fileName;
    }

    /**
     * Upload a category image
     */
    public String uploadCategoryImage(MultipartFile file) throws IOException {
        validateFile(file);

        String fileName = generateFileName(file);
        uploadedImageService.save(fileName, file);

        try {
            Path uploadPath = createUploadDirectory("categories");
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("File-system cache write failed for category image " + fileName + ": " + e.getMessage());
        }

        return "categories/" + fileName;
    }

    /**
     * Upload multiple product images
     */
    public List<String> uploadMultipleProductImages(List<MultipartFile> files) throws IOException {
        if (files.size() > 10) {
            throw new IllegalArgumentException("Maximum 10 images allowed per product");
        }

        return files.stream()
                .map(file -> {
                    try {
                        return uploadProductImage(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to upload image: " + file.getOriginalFilename(), e);
                    }
                })
                .toList();
    }

    /**
     * Delete a file
     */
    public boolean deleteFile(String relativePath) {
        String fileName = uploadedImageService.extractFileName(relativePath);
        try {
            Path filePath = Paths.get(uploadDir, relativePath);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        } finally {
            uploadedImageService.deleteByFileName(fileName);
        }
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name is required");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + ALLOWED_EXTENSIONS);
        }

        String mimeType = file.getContentType();
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("MIME type not allowed. Allowed types: " + ALLOWED_MIME_TYPES);
        }
    }

    /**
     * Create upload directory
     */
    private Path createUploadDirectory(String subDir) throws IOException {
        Path uploadPath = Paths.get(uploadDir, subDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        return uploadPath;
    }

    /**
     * Generate unique filename
     */
    private String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s.%s", timestamp, uuid, extension);
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Get file URL for serving
     */
    public String getFileUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        String normalizedPrefix = publicUrlPrefix.endsWith("/") ? publicUrlPrefix : publicUrlPrefix + "/";
        return normalizedPrefix + relativePath.replace("\\", "/");
    }
}
