package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.UploadedImage;
import com.ecommerse.backend.repositories.UploadedImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Service
public class UploadedImageService {

    private final UploadedImageRepository uploadedImageRepository;

    public UploadedImageService(UploadedImageRepository uploadedImageRepository) {
        this.uploadedImageRepository = uploadedImageRepository;
    }

    public void save(String fileName, MultipartFile file) throws IOException {
        if (fileName == null || fileName.isBlank() || file == null || file.isEmpty()) {
            return;
        }

        save(fileName, file.getBytes(), file.getContentType());
    }

    public void save(String fileName, byte[] imageData, String contentType) {
        if (fileName == null || fileName.isBlank() || imageData == null || imageData.length == 0) {
            return;
        }

        UploadedImage uploadedImage = new UploadedImage(fileName, imageData, resolveContentType(contentType, fileName));
        uploadedImageRepository.save(uploadedImage);
    }

    public Optional<UploadedImage> findByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }
        return uploadedImageRepository.findById(fileName);
    }

    public void deleteByFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        uploadedImageRepository.deleteById(fileName);
    }

    public String extractFileName(String imagePathOrUrl) {
        if (imagePathOrUrl == null || imagePathOrUrl.isBlank()) {
            return null;
        }
        int slashIndex = imagePathOrUrl.lastIndexOf('/');
        return slashIndex >= 0 ? imagePathOrUrl.substring(slashIndex + 1) : imagePathOrUrl;
    }

    private String resolveContentType(String providedType, String fileName) {
        if (providedType != null && !providedType.isBlank()) {
            return providedType;
        }

        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
