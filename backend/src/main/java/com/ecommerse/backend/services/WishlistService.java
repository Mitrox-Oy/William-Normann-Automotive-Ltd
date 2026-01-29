package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductDTO;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.entities.Wishlist;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.repositories.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service class for managing user wishlists
 */
@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Autowired
    public WishlistService(WishlistRepository wishlistRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    /**
     * Get user's wishlist with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductDTO> getUserWishlist(String username, Pageable pageable) {
        Page<Wishlist> wishlistItems = wishlistRepository.findByUserUsernameWithProductDetails(username, pageable);
        return wishlistItems.map(wishlist -> convertProductToDTO(wishlist.getProduct()));
    }

    /**
     * Add product to user's wishlist
     */
    public boolean addToWishlist(String username, Long productId) {
        // Check if product exists and is active
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or inactive"));

        // Check if already in wishlist
        if (wishlistRepository.existsByUserUsernameAndProductId(username, productId)) {
            return false; // Already exists
        }

        // Get user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Create wishlist entry
        Wishlist wishlistItem = new Wishlist(user, product);
        wishlistRepository.save(wishlistItem);
        return true;
    }

    /**
     * Remove product from user's wishlist
     */
    public boolean removeFromWishlist(String username, Long productId) {
        int deletedCount = wishlistRepository.deleteByUserUsernameAndProductId(username, productId);
        return deletedCount > 0;
    }

    /**
     * Check if product is in user's wishlist
     */
    @Transactional(readOnly = true)
    public boolean isInWishlist(String username, Long productId) {
        return wishlistRepository.existsByUserUsernameAndProductId(username, productId);
    }

    /**
     * Clear all items from user's wishlist
     */
    public int clearWishlist(String username) {
        return wishlistRepository.deleteByUserUsername(username);
    }

    /**
     * Get count of items in user's wishlist
     */
    @Transactional(readOnly = true)
    public long getWishlistCount(String username) {
        return wishlistRepository.countByUserUsername(username);
    }

    /**
     * Convert Product entity to ProductDTO
     */
    private ProductDTO convertProductToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setSku(product.getSku());
        dto.setImageUrl(product.getImageUrl());
        dto.setActive(product.getActive());
        dto.setFeatured(product.getFeatured());
        dto.setWeight(product.getWeight());
        dto.setBrand(product.getBrand());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryName(product.getCategory().getName());
        dto.setInStock(product.isInStock());
        dto.setAvailable(product.isAvailable());
        dto.setCreatedDate(product.getCreatedDate());
        dto.setUpdatedDate(product.getUpdatedDate());
        return dto;
    }
}
