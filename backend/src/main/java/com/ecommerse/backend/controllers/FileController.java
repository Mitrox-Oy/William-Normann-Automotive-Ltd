package com.ecommerse.backend.controllers;

import com.ecommerse.backend.services.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for file upload and serving operations
 */
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
@Tag(name = "Files", description = "File upload and serving operations")
public class FileController {

    private final FileService fileService;

    // Base upload directory
    private final String uploadDir = "uploads";

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @Operation(summary = "Upload product image", description = "Upload a single product image. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Image uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or file too large"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "500", description = "File upload failed")
    })
    @PostMapping("/products/image")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Map<String, String>> uploadProductImage(
            @Parameter(description = "Image file to upload", required = true) @RequestParam("file") MultipartFile file) {

        try {
            String relativePath = fileService.uploadProductImage(file);
            String fileUrl = fileService.getFileUrl(relativePath);

            Map<String, String> response = new HashMap<>();
            response.put("relativePath", relativePath);
            response.put("url", fileUrl);
            response.put("message", "File uploaded successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to upload file");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(summary = "Upload multiple product images", description = "Upload multiple product images at once. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Images uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid files or too many files"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "500", description = "File upload failed")
    })
    @PostMapping("/products/images")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadMultipleProductImages(
            @Parameter(description = "Image files to upload", required = true) @RequestParam("files") List<MultipartFile> files) {

        try {
            List<String> relativePaths = fileService.uploadMultipleProductImages(files);
            List<String> fileUrls = relativePaths.stream()
                    .map(fileService::getFileUrl)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("relativePaths", relativePaths);
            response.put("urls", fileUrls);
            response.put("message", files.size() + " files uploaded successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to upload files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Failed to upload one or more files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @Operation(summary = "Serve uploaded file", description = "Serve an uploaded file by its relative path. Available to all users.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File served successfully"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Failed to read file")
    })
    @GetMapping("/{subDir}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @Parameter(description = "Subdirectory (e.g., 'products')", example = "products", required = true) @PathVariable String subDir,
            @Parameter(description = "Filename", example = "20250903_123456_abc12345.jpg", required = true) @PathVariable String filename) {

        try {
            Path filePath = Paths.get(uploadDir, subDir, filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Delete file", description = "Delete an uploaded file. Requires OWNER role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "File deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires OWNER role"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @DeleteMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "Relative path of file to delete", example = "products/20250903_123456_abc12345.jpg", required = true) @RequestParam String relativePath) {

        boolean deleted = fileService.deleteFile(relativePath);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}



