package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.ProductDTO;
import com.ecommerse.backend.dto.ProductVariantResponse;
import com.ecommerse.backend.services.ProductService;
import com.ecommerse.backend.services.ProductVariantService;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private ProductVariantService productVariantService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
    }

    @Test
    void getAllProducts_ShouldForwardPaginationAndFiltersToService() throws Exception {
        ProductDTO dto = new ProductDTO();
        dto.setId(42L);
        dto.setName("Test Product");

        Page<ProductDTO> page = new PageImpl<>(
                List.of(dto),
                PageRequest.of(0, 12, Sort.by("price").ascending()),
                1
        );

        when(productService.getCatalogProducts(any(Pageable.class), eq(3L), eq("iPhone"))).thenReturn(page);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "12")
                        .param("sortBy", "price")
                        .param("sortDir", "asc")
                        .param("category", "3")
                        .param("search", "iPhone")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getCatalogProducts(pageableCaptor.capture(), eq(3L), eq("iPhone"));

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(12);
        Sort.Order order = pageable.getSort().getOrderFor("price");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getProductVariants_ShouldDelegateToService() throws Exception {
        Long productId = 5L;
        ProductVariantResponse response = new ProductVariantResponse();
        response.setId(10L);
        response.setName("Variant A");

        when(productVariantService.listVariants(productId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/products/{productId}/variants", productId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(productVariantService).listVariants(productId);
    }
}
