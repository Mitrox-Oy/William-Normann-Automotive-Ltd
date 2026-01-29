package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductDTO;
import com.ecommerse.backend.entities.Category;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.entities.Wishlist;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.repositories.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    private static final String USERNAME = "customer@example.com";

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);
        user.setUsername(USERNAME);

        Category category = new Category();
        category.setId(3L);
        category.setName("Accessories");
        category.setSlug("accessories");

        product = new Product();
        product.setId(11L);
        product.setName("Leather Wallet");
        product.setDescription("Slim leather wallet");
        product.setPrice(BigDecimal.valueOf(49.99));
        product.setStockQuantity(8);
        product.setSku("WALLET-001");
        product.setImageUrl("wallet.jpg");
        product.setActive(true);
        product.setFeatured(false);
        product.setCategory(category);
        product.setCreatedDate(LocalDateTime.now().minusDays(1));
        product.setUpdatedDate(LocalDateTime.now());
    }

    @Test
    void addToWishlistPersistsWhenProductNotAlreadySaved() {
        when(productRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUserUsernameAndProductId(USERNAME, 11L)).thenReturn(false);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        boolean added = wishlistService.addToWishlist(USERNAME, 11L);

        assertThat(added).isTrue();
        ArgumentCaptor<Wishlist> wishlistCaptor = ArgumentCaptor.forClass(Wishlist.class);
        verify(wishlistRepository).save(wishlistCaptor.capture());
        Wishlist saved = wishlistCaptor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getProduct()).isSameAs(product);
    }

    @Test
    void addToWishlistShortCircuitsWhenAlreadyPresent() {
        when(productRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.of(product));
        when(wishlistRepository.existsByUserUsernameAndProductId(USERNAME, 11L)).thenReturn(true);

        boolean added = wishlistService.addToWishlist(USERNAME, 11L);

        assertThat(added).isFalse();
        verify(wishlistRepository, never()).save(any(Wishlist.class));
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void addToWishlistThrowsWhenProductMissingOrInactive() {
        when(productRepository.findByIdAndActiveTrue(11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wishlistService.addToWishlist(USERNAME, 11L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getUserWishlistMapsPageToProductDto() {
        Wishlist wishlist = new Wishlist(user, product);
        wishlist.setId(21L);
        Page<Wishlist> page = new PageImpl<>(List.of(wishlist));
        Pageable pageable = PageRequest.of(0, 10);

        when(wishlistRepository.findByUserUsernameWithProductDetails(USERNAME, pageable)).thenReturn(page);

        Page<ProductDTO> result = wishlistService.getUserWishlist(USERNAME, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        ProductDTO dto = result.getContent().get(0);
        assertThat(dto.getId()).isEqualTo(product.getId());
        assertThat(dto.getName()).isEqualTo(product.getName());
        assertThat(dto.getCategoryId()).isEqualTo(product.getCategory().getId());
    }

    @Test
    void removeFromWishlistReturnsTrueWhenDeleted() {
        when(wishlistRepository.deleteByUserUsernameAndProductId(USERNAME, 11L)).thenReturn(1);

        boolean removed = wishlistService.removeFromWishlist(USERNAME, 11L);

        assertThat(removed).isTrue();
    }

    @Test
    void clearWishlistDelegatesToRepository() {
        when(wishlistRepository.deleteByUserUsername(USERNAME)).thenReturn(5);

        int removed = wishlistService.clearWishlist(USERNAME);

        assertThat(removed).isEqualTo(5);
        verify(wishlistRepository).deleteByUserUsername(USERNAME);
    }

    @Test
    void isInWishlistChecksRepository() {
        when(wishlistRepository.existsByUserUsernameAndProductId(USERNAME, 11L)).thenReturn(true);

        boolean result = wishlistService.isInWishlist(USERNAME, 11L);

        assertThat(result).isTrue();
    }

    @Test
    void getWishlistCountReturnsRepositoryValue() {
        when(wishlistRepository.countByUserUsername(USERNAME)).thenReturn(4L);

        long count = wishlistService.getWishlistCount(USERNAME);

        assertThat(count).isEqualTo(4L);
    }
}
