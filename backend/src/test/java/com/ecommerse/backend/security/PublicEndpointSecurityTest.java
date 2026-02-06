package com.ecommerse.backend.security;

import com.ecommerse.backend.controllers.CategoryController;
import com.ecommerse.backend.controllers.ProductController;
import com.ecommerse.backend.services.CategoryService;
import com.ecommerse.backend.services.FileService;
import com.ecommerse.backend.services.ProductService;
import com.ecommerse.backend.services.UserDetailsServiceImpl;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProductController.class, CategoryController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "cors.allowed-origins=http://localhost:4200",
        "jwt.secret=test-secret-key-which-is-long-enough-1234567890",
        "jwt.expiration=3600000",
        "spring.jpa.open-in-view=false",
        "spring.data.jpa.repositories.enabled=false",
        "spring.main.allow-bean-definition-overriding=true"
})
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
})
class PublicEndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private com.ecommerse.backend.services.ProductVariantService productVariantService;

    @MockBean
    private FileService fileService;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockBean(name = "entityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @MockBean(name = "transactionManager")
    private PlatformTransactionManager transactionManager;

    @Test
    void anonymousUserCanFetchProducts() throws Exception {
        when(productService.getAllProducts(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUserCanFetchCategories() throws Exception {
        when(categoryService.getAllActiveCategories()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());
    }

}
