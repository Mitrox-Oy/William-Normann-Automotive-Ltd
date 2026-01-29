package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.ProductDTO;
import com.ecommerse.backend.services.WishlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistControllerTest {

    private static final String USERNAME = "customer@example.com";

    @Mock
    private WishlistService wishlistService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WishlistController wishlistController;

    @BeforeEach
    void setUp() {
        when(authentication.getName()).thenReturn(USERNAME);
    }

    @Test
    void getWishlistReturnsPagedProducts() {
        ProductDTO product = new ProductDTO();
        product.setId(1L);
        product.setName("Wireless Headphones");
        product.setPrice(BigDecimal.valueOf(149.99));
        Page<ProductDTO> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);
        when(wishlistService.getUserWishlist(eq(USERNAME), any())).thenReturn(page);

        ResponseEntity<Page<ProductDTO>> response = wishlistController.getWishlist(authentication, 0, 10);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).containsExactly(product);
        verify(wishlistService).getUserWishlist(eq(USERNAME), any());
    }

    @Test
    void addToWishlistReturnsSuccessWhenItemAdded() {
        when(wishlistService.addToWishlist(USERNAME, 5L)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = wishlistController.addToWishlist(authentication, 5L);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("inWishlist")).isEqualTo(true);
        assertThat(response.getBody().get("message")).isEqualTo("Product added to wishlist");
    }

    @Test
    void addToWishlistReturnsAlreadyInWishlistMessageWhenServiceReturnsFalse() {
        when(wishlistService.addToWishlist(USERNAME, 5L)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = wishlistController.addToWishlist(authentication, 5L);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("inWishlist")).isEqualTo(true);
        assertThat(response.getBody().get("message")).isEqualTo("Product already in wishlist");
    }

    @Test
    void addToWishlistHandlesIllegalArgumentWithBadRequest() {
        when(wishlistService.addToWishlist(USERNAME, 9L))
                .thenThrow(new IllegalArgumentException("Product not found"));

        ResponseEntity<Map<String, Object>> response = wishlistController.addToWishlist(authentication, 9L);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Product not found");
    }

    @Test
    void removeFromWishlistReturnsSuccessWhenItemRemoved() {
        when(wishlistService.removeFromWishlist(USERNAME, 7L)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = wishlistController.removeFromWishlist(authentication, 7L);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("inWishlist")).isEqualTo(false);
        assertThat(response.getBody().get("message")).isEqualTo("Product removed from wishlist");
    }

    @Test
    void removeFromWishlistStillReturnsOkWhenItemMissing() {
        when(wishlistService.removeFromWishlist(USERNAME, 7L)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = wishlistController.removeFromWishlist(authentication, 7L);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Product not found in wishlist");
    }

    @Test
    void isInWishlistDelegatesToService() {
        when(wishlistService.isInWishlist(USERNAME, 2L)).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> response = wishlistController.isInWishlist(authentication, 2L);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("inWishlist")).isTrue();
        verify(wishlistService).isInWishlist(USERNAME, 2L);
    }

    @Test
    void clearWishlistReturnsRemovalCount() {
        when(wishlistService.clearWishlist(USERNAME)).thenReturn(3);

        ResponseEntity<Map<String, String>> response = wishlistController.clearWishlist(authentication);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Wishlist cleared. Removed 3 items.");
    }

    @Test
    void getWishlistCountReturnsCountFromService() {
        when(wishlistService.getWishlistCount(USERNAME)).thenReturn(4L);

        ResponseEntity<Map<String, Long>> response = wishlistController.getWishlistCount(authentication);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("count")).isEqualTo(4L);
        verify(wishlistService).getWishlistCount(USERNAME);
    }
}

