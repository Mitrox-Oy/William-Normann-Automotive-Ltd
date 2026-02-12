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

        boolean shouldBeMain = currentImageCount == 0 || (isMain != null && isMain);

        // Position policy: main image is always position 0 so shop cards and detail pages show it first.
        int position;
        if (shouldBeMain) {
            // Shift existing images down (position +1) to make room at 0.
            if (currentImageCount > 0) {
                productImageRepository.incrementPositionsFromPosition(productId, 0);
            }
            productImageRepository.clearMainImageForProduct(productId);
            position = 0;
            isMain = true;
        } else {
            position = (int) currentImageCount;
            isMain = false;
        }

        // Create and save ProductImage
        ProductImage productImage = new ProductImage(imageUrl, position, isMain, product);
        ProductImage saved = productImageRepository.save(productImage);

        // Backwards-compatibility: some frontends still read Product.imageUrl.
        if (Boolean.TRUE.equals(isMain)) {
            product.setImageUrl(imageUrl);
            productRepository.save(product);
        }

        return saved;
    }

    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with id: " + imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Image does not belong to the specified product");
        }

        Integer deletedPosition = image.getPosition();
        boolean wasMain = Boolean.TRUE.equals(image.getIsMain());
        String deletedUrl = image.getImageUrl();

        productImageRepository.delete(image);

        // Delete physical file
        deleteImageFileQuietly(deletedUrl);

        // Adjust positions of images after deleted image (best-effort; older data can have null positions).
        if (deletedPosition != null) {
            productImageRepository.decrementPositionsAfterPosition(productId, deletedPosition);
        }

        // If the deleted image was main, ensure another remaining image becomes main (and therefore position 0).
        List<ProductImage> remaining = productImageRepository.findByProductIdOrderByPositionAsc(productId);
        if (remaining.isEmpty()) {
            // Keep legacy imageUrl consistent to avoid broken storefront thumbnails.
            productRepository.findById(productId).ifPresent(p -> {
                p.setImageUrl(null);
                productRepository.save(p);
            });
            return;
        }

        if (wasMain) {
            setMainImage(productId, remaining.get(0).getId());
            return;
        }

        // Enforce policy: main image is always position 0. If data got out of sync, fix it.
        ProductImage main = remaining.stream().filter(img -> Boolean.TRUE.equals(img.getIsMain())).findFirst().orElse(null);
        if (main == null) {
            setMainImage(productId, remaining.get(0).getId());
        } else if (main.getPosition() == null || main.getPosition() != 0) {
            setMainImage(productId, main.getId());
        }
    }

    public void setMainImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with id: " + imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Image does not belong to the specified product");
        }

        // Main image must be position 0, so reorder positions accordingly (max 10 images -> safe to rewrite).
        List<ProductImage> allImages = productImageRepository.findByProductIdOrderByPositionAsc(productId);
        List<ProductImage> reordered = new java.util.ArrayList<>();
        for (ProductImage img : allImages) {
            if (img.getId().equals(imageId)) {
                reordered.add(img);
            }
        }
        for (ProductImage img : allImages) {
            if (!img.getId().equals(imageId)) {
                reordered.add(img);
            }
        }

        for (int i = 0; i < reordered.size(); i++) {
            ProductImage img = reordered.get(i);
            img.setPosition(i);
            img.setIsMain(img.getId().equals(imageId));
            productImageRepository.save(img);
        }

        // Backwards-compatibility: keep legacy Product.imageUrl in sync with main selection.
        Product product = image.getProduct();
        if (product != null) {
            product.setImageUrl(image.getImageUrl());
            productRepository.save(product);
        }
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

    public ProductImage replaceImage(Long productId, Long imageId, MultipartFile file) throws IOException {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found with id: " + imageId));

        if (!image.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Image does not belong to the specified product");
        }

        validateImageFile(file);

        // Save the new file first, then swap the URL, then clean up the old file.
        String oldUrl = image.getImageUrl();
        String fileName = saveImageFile(file);
        uploadedImageService.save(fileName, file);
        String imageUrl = "/api/images/" + fileName;

        image.setImageUrl(imageUrl);
        ProductImage saved = productImageRepository.save(image);

        // Best-effort cleanup of old file/storage reference.
        if (oldUrl != null && !oldUrl.isBlank()) {
            deleteImageFileQuietly(oldUrl);
        }

        // Backwards-compatibility: if we replaced the main image file, keep Product.imageUrl pointing at it.
        if (Boolean.TRUE.equals(saved.getIsMain()) && saved.getProduct() != null) {
            Product product = saved.getProduct();
            product.setImageUrl(saved.getImageUrl());
            productRepository.save(product);
        }

        return saved;
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

        // Enforce: main image is always position 0.
        ProductImage main = productImageRepository.findByProductIdAndIsMainTrue(productId).orElse(null);
        if (main != null && main.getPosition() != null && main.getPosition() != 0) {
            setMainImage(productId, main.getId());
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

    private void deleteImageFileQuietly(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String fileName = uploadedImageService.extractFileName(imageUrl);
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        try {
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but don't throw exception
            System.err.println("Failed to delete image file: " + imageUrl);
        }

        // DB cleanup should not be allowed to fail the API call.
        try {
            uploadedImageService.deleteByFileName(fileName);
        } catch (RuntimeException e) {
            System.err.println("Failed to delete uploaded image DB row for fileName=" + fileName + ": " + e.getMessage());
        }
    }
}
