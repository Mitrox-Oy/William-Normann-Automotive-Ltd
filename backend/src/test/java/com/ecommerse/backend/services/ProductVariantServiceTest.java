package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ProductVariantRequest;
import com.ecommerse.backend.dto.ProductVariantResponse;
import com.ecommerse.backend.entities.Product;
import com.ecommerse.backend.entities.ProductVariant;
import com.ecommerse.backend.repositories.ProductRepository;
import com.ecommerse.backend.repositories.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private ProductVariantService productVariantService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(100L);
        product.setStockQuantity(0);
    }

    @Test
    void createVariant_ShouldSetDefaultAndUpdateStock() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productVariantRepository.existsBySku("NEW-SKU")).thenReturn(false);
        when(productVariantRepository.findTopByProductIdOrderByPositionDesc(product.getId())).thenReturn(Optional.empty());
        when(productVariantRepository.countByProductIdAndDefaultVariantTrue(product.getId())).thenReturn(0L);
        when(productVariantRepository.sumActiveStockByProductId(product.getId())).thenReturn(7);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.findByProductIdOrderByPositionAsc(product.getId()))
                .thenAnswer(invocation -> {
                    ProductVariant variant = new ProductVariant();
                    variant.setId(10L);
                    variant.setProduct(product);
                    variant.setName("Test Variant");
                    variant.setSku("NEW-SKU");
                    variant.setPosition(0);
                    variant.setDefaultVariant(true);
                    variant.setStockQuantity(7);
                    return List.of(variant);
                });
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
            ProductVariant variant = invocation.getArgument(0);
            if (variant.getId() == null) {
                variant.setId(10L);
            }
            return variant;
        });

        ProductVariantRequest request = new ProductVariantRequest();
        request.setName("Test Variant");
        request.setSku("NEW-SKU");
        request.setStockQuantity(7);

        ProductVariantResponse response = productVariantService.createVariant(product.getId(), request);

        assertThat(response.getDefaultVariant()).isTrue();

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getStockQuantity()).isEqualTo(7);
    }

    @Test
    void deleteVariant_ShouldPromoteNextVariantToDefault() {
        ProductVariant defaultVariant = new ProductVariant();
        defaultVariant.setId(21L);
        defaultVariant.setProduct(product);
        defaultVariant.setDefaultVariant(true);
        defaultVariant.setStockQuantity(5);

        ProductVariant secondVariant = new ProductVariant();
        secondVariant.setId(22L);
        secondVariant.setProduct(product);
        secondVariant.setDefaultVariant(false);
        secondVariant.setStockQuantity(3);

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndProductId(defaultVariant.getId(), product.getId()))
                .thenReturn(Optional.of(defaultVariant));
        doNothing().when(productVariantRepository).delete(defaultVariant);
        doNothing().when(productVariantRepository).flush();
        when(productVariantRepository.countByProductIdAndDefaultVariantTrue(product.getId())).thenReturn(0L);
        when(productVariantRepository.findByProductIdOrderByPositionAsc(product.getId())).thenReturn(List.of(secondVariant));
        when(productVariantRepository.save(secondVariant)).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(productVariantRepository.sumActiveStockByProductId(product.getId())).thenReturn(3);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productVariantService.deleteVariant(product.getId(), defaultVariant.getId());

        assertThat(secondVariant.getDefaultVariant()).isTrue();

        verify(productVariantRepository, times(1)).save(secondVariant);
        verify(productRepository).save(eq(product));
        assertThat(product.getStockQuantity()).isEqualTo(3);
    }
}
