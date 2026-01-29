package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.Cart;
import com.ecommerse.backend.entities.CartItem;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.repositories.CartItemRepository;
import com.ecommerse.backend.repositories.CartRepository;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceReservationTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void releaseExpiredReservationsRestoresStockAndRemovesItems() {
        Cart cart = new Cart();
        cart.setId(101L);

        Product product = new Product();
        product.setId(55L);
        product.setName("Demo");
        product.setSku("SKU-55");
        product.setPrice(new BigDecimal("42.00"));
        product.setStockQuantity(3);

        CartItem cartItem = new CartItem();
        cartItem.setId(200L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setReservedUntil(LocalDateTime.now().minusMinutes(15));
        cart.getItems().add(cartItem);

        when(cartItemRepository.findExpiredReservations(any(LocalDateTime.class)))
                .thenReturn(List.of(cartItem));
        when(productRepository.save(product)).thenReturn(product);
        when(cartRepository.save(cart)).thenReturn(cart);

        int released = cartService.releaseExpiredReservations();

        assertEquals(1, released);
        assertEquals(5, product.getStockQuantity(), "Stock should be restored after releasing reservation");
        verify(cartItemRepository).delete(cartItem);
        verify(productRepository).save(product);
        verify(cartRepository).save(cart);
    }
}
