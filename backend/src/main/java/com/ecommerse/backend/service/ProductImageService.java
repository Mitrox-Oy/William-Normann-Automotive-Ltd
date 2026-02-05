package com.ecommerse.backend.service;

import com.ecommerse.backend.dto.ImagePositionDTO;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.entities.ProductImage;
import com.ecommerse.backend.repositories.ProductImageRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.services.UploadedImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProductImageService {

    private static final String UPLOAD_DIR = "uploads/products/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };
    private static final int MAX_IMAGES_PER_PRODUCT = 10;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UploadedImageService uploadedImageService;

    public List<ProductImage> getProductImages(Long productId) {
        return productImageRepository.findByProductIdOrderByPositionAsc(productId);
    }

    public ProductImage uploadImage(Long productId, MultipartFile file, Boolean isMain) throws IOException {
        // Validate product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        // Check image limit
        long currentImageCount = productImageRepository.countByProductId(productId);
        if (currentImageCount >= MAX_IMAGES_PER_PRODUCT) {
            throw new IllegalArgumentException("Maximum " + MAX_IMAGES_PER_PRODUCT + " images allowed per product");
        }

        // Validate file
        validateImageFile(file);

        // Save file
        String fileName = saveImageFile(file);
        uploadedImageService.save(fileName, file);
        String imageUrl = "/api/images/" + fileName;

        // Calculate position
        int position = (int) currentImageCount;

        // If this is the first image or explicitly set as main, make it main
        if (currentImageCount == 0 || (isMain != null && isMain)) {
            // Clear existing main image
            productImageRepository.clearMainImageForProduct(productId);
            isMain = true;
        } else {
            isMain = false;
        }

        // Create and save ProductImage
        ProductImage productImage = new ProductImage(imageUrl, position, isMain, product);
        return productImageRepository.save(productImage);
    }

    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with id: " + imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Image does not belong to the specified product");
        }

        // Check if at least one image will remain (business rule)
        long remainingImages = productImageRepository.countByProductId(productId) - 1;
        if (remainingImages == 0) {
            throw new IllegalArgumentException("Cannot delete the last image. Products must have at least one image.");
        }

        // If deleting main image, set another image as main
        if (image.getIsMain()) {
            List<ProductImage> otherImages = productImageRepository.findByProductIdOrderByPositionAsc(productId);
            otherImages.stream()
                    .filter(img -> !img.getId().equals(imageId))
                    .findFirst()
                    .ifPresent(img -> {
                        img.setIsMain(true);
                        productImageRepository.save(img);
                    });
        }

        // Adjust positions of images after deleted image
        productImageRepository.decrementPositionsAfterPosition(productId, image.getPosition());

        // Delete the image
        productImageRepository.delete(image);

        // Delete physical file
        deleteImageFile(image.getImageUrl());
    }

    public void setMainImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with id: " + imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Image does not belong to the specified product");
        }

        // Clear existing main image
        productImageRepository.clearMainImageForProduct(productId);

        // Set new main image
        image.setIsMain(true);
        productImageRepository.save(image);
    }

    public void updateImagePosition(Long productId, Long imageId, Integer newPosition) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with id: " + imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Image does not belong to the specified product");
        }

        if (newPosition < 0 || newPosition >= MAX_IMAGES_PER_PRODUCT) {
            throw new IllegalArgumentException("Position must be between 0 and " + (MAX_IMAGES_PER_PRODUCT - 1));
        }

        Integer oldPosition = image.getPosition();
        if (oldPosition.equals(newPosition)) {
            return; // No change needed
        }

        // Update positions of other images
        List<ProductImage> allImages = productImageRepository.findByProductIdOrderByPositionAsc(productId);

        if (newPosition > oldPosition) {
            // Moving down: shift images up
            for (ProductImage img : allImages) {
                if (img.getPosition() > oldPosition && img.getPosition() <= newPosition) {
                    img.setPosition(img.getPosition() - 1);
                    productImageRepository.save(img);
                }
            }
        } else {
            // Moving up: shift images down
            for (ProductImage img : allImages) {
                if (img.getPosition() >= newPosition && img.getPosition() < oldPosition) {
                    img.setPosition(img.getPosition() + 1);
                    productImageRepository.save(img);
                }
            }
        }

        // Update the target image position
        image.setPosition(newPosition);
        productImageRepository.save(image);
    }

    public List<ProductImage> reorderImages(Long productId, List<ImagePositionDTO> newOrder) {
        // Validate all images belong to the product
        for (ImagePositionDTO dto : newOrder) {
            ProductImage image = productImageRepository.findById(dto.getImageId())
                    .orElseThrow(() -> new IllegalArgumentException("Image not found with id: " + dto.getImageId()));

            if (!image.getProduct().getId().equals(productId)) {
                throw new IllegalArgumentException(
                        "Image " + dto.getImageId() + " does not belong to product " + productId);
            }
        }

        // Update positions
        for (ImagePositionDTO dto : newOrder) {
            ProductImage image = productImageRepository.findById(dto.getImageId()).get();
            image.setPosition(dto.getPosition());
            productImageRepository.save(image);
        }

        return productImageRepository.findByProductIdOrderByPositionAsc(productId);
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("File name is null");
        }

        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        boolean validExtension = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (extension.equals(allowedExt)) {
                validExtension = true;
                break;
            }
        }

        if (!validExtension) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: jpg, jpeg, png, gif, webp");
        }
    }

    private String saveImageFile(MultipartFile file) {
        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        String fileName = UUID.randomUUID().toString() + extension;

        // Keep a filesystem copy as a best-effort cache.
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("File-system cache write failed for product image " + fileName + ": " + e.getMessage());
        }

        return fileName;
    }

    private void deleteImageFile(String imageUrl) {
        String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        try {
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but don't throw exception
            System.err.println("Failed to delete image file: " + imageUrl);
        } finally {
            uploadedImageService.deleteByFileName(fileName);
        }
    }
}
