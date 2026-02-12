package com.ecommerse.backend.controller;

import com.ecommerse.backend.dto.ImagePositionDTO;
import com.ecommerse.backend.dto.ProductImageResponse;
import com.ecommerse.backend.entities.ProductImage;
import com.ecommerse.backend.service.ProductImageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/images")
@CrossOrigin(origins = "*")
public class ProductImageController {

    @Autowired
    private ProductImageService productImageService;

    @GetMapping
    public ResponseEntity<List<ProductImageResponse>> getProductImages(@PathVariable Long productId) {
        List<ProductImage> images = productImageService.getProductImages(productId);
        List<ProductImageResponse> responses = images.stream()
                .map(ProductImageResponse::new)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductImageResponse> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") Boolean isMain) {
        try {
            ProductImage uploadedImage = productImageService.uploadImage(productId, file, isMain);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ProductImageResponse(uploadedImage));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{imageId}/position")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> updateImagePosition(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @RequestParam Integer newPosition) {
        try {
            productImageService.updateImagePosition(productId, imageId, newPosition);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{imageId}/main")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> setMainImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        try {
            productImageService.setMainImage(productId, imageId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<?> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        try {
            productImageService.deleteImage(productId, imageId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{imageId}/replace")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ProductImageResponse> replaceImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @RequestParam("file") MultipartFile file) {
        try {
            ProductImage replaced = productImageService.replaceImage(productId, imageId, file);
            return ResponseEntity.ok(new ProductImageResponse(replaced));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<ProductImageResponse>> reorderImages(
            @PathVariable Long productId,
            @RequestBody @Valid List<ImagePositionDTO> newOrder) {
        try {
            List<ProductImage> reorderedImages = productImageService.reorderImages(productId, newOrder);
            List<ProductImageResponse> responses = reorderedImages.stream()
                    .map(ProductImageResponse::new)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
