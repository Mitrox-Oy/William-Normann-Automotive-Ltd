package com.ecommerse.backend.controller;

import com.ecommerse.backend.entities.UploadedImage;
import com.ecommerse.backend.services.UploadedImageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    private final UploadedImageService uploadedImageService;

    public ImageController(UploadedImageService uploadedImageService) {
        this.uploadedImageService = uploadedImageService;
    }

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        try {
            if (fileName.contains("..")) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = resolveImagePath(fileName);
            if (filePath == null) {
                return loadImageFromDatabase(fileName);
            }
            File file = filePath.toFile();

            if (!file.exists() || !file.isFile()) {
                return loadImageFromDatabase(fileName);
            }

            Resource resource = new FileSystemResource(file);

            // Determine content type
            String contentType = getContentType(fileName);
            cacheImageInDatabaseIfMissing(fileName, filePath, contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<Resource> loadImageFromDatabase(String fileName) {
        return uploadedImageService.findByFileName(fileName)
                .map(this::toImageResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<Resource> toImageResponse(UploadedImage uploadedImage) {
        Resource resource = new ByteArrayResource(uploadedImage.getImageData());
        String contentType = uploadedImage.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = getContentType(uploadedImage.getFileName());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + uploadedImage.getFileName() + "\"")
                .body(resource);
    }

    private void cacheImageInDatabaseIfMissing(String fileName, Path filePath, String contentType) {
        if (uploadedImageService.findByFileName(fileName).isPresent()) {
            return;
        }

        try {
            uploadedImageService.save(fileName, Files.readAllBytes(filePath), contentType);
        } catch (IOException e) {
            // Keep serving from disk even if DB backfill fails.
            System.err.println("Failed to backfill image " + fileName + " into database: " + e.getMessage());
        }
    }

    private String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        switch (extension) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }

    private Path resolveImagePath(String fileName) {
        List<Path> candidates = List.of(
                Paths.get(uploadDir, "products", fileName),
                Paths.get("uploads", "products", fileName),
                Paths.get("backend", "uploads", "products", fileName),
                Paths.get(System.getProperty("user.dir"), "uploads", "products", fileName),
                Paths.get("/app/uploads/products", fileName));

        for (Path candidate : candidates) {
            if (candidate.toFile().exists()) {
                return candidate;
            }
        }

        return null;
    }
}
