package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.CartDTO;
import com.ecommerse.backend.dto.CartItemDTO;
import com.ecommerse.backend.dto.CartValidationResult;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.UserRepository;
import com.ecommerse.backend.services.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartController cartController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCartReturnsCartForAuthenticatedUser() {
        User principal = new User();
        principal.setId(42L);
        principal.setUsername("customer@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        CartDTO cart = new CartDTO();
        when(cartService.getCartByUserId(42L)).thenReturn(cart);

        ResponseEntity<CartDTO> response = cartController.getCart();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(cart);
        verify(cartService).getCartByUserId(42L);
    }

    @Test
    void addItemToCartResolvesUserIdFromRepositoryWhenPrincipalIsUsername() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                )
        );

        User storedUser = new User();
        storedUser.setId(99L);
        storedUser.setUsername("user@example.com");
        when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.of(storedUser));
        when(cartService.isCartFull(99L)).thenReturn(false);
        CartItemDTO item = new CartItemDTO();
        item.setProductId(10L);
        when(cartService.addItemToCart(99L, 10L, 1)).thenReturn(item);

        CartController.AddToCartRequest request = new CartController.AddToCartRequest();
        request.setProductId(10L);
        request.setQuantity(1);

        ResponseEntity<Map<String, Object>> response = cartController.addItemToCart(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        verify(cartService).addItemToCart(99L, 10L, 1);
    }

    @Test
    void validateCartReturnsSummaryForAuthenticatedUser() {
        User principal = new User();
        principal.setId(77L);
        principal.setUsername("cart-user@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        CartValidationResult validation = new CartValidationResult();
        validation.setValid(false);
        validation.setErrors(List.of("Price changed"));
        validation.setCartExpired(false);
        validation.setCartActive(true);
        validation.setTotalItems(3);

        when(cartService.validateCartComprehensive(77L)).thenReturn(validation);

        ResponseEntity<CartValidationResult> response = cartController.validateCart();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(validation);
        verify(cartService).validateCartComprehensive(77L);
    }

    @Test
    void validateCartWrapsUnexpectedExceptions() {
        User principal = new User();
        principal.setId(88L);
        principal.setUsername("cart-user@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(cartService.validateCartComprehensive(88L)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(cartController::validateCart)
                .hasMessageContaining("Failed to validate cart")
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
