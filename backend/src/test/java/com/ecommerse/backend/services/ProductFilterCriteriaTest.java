package com.ecommerse.backend.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductFilterCriteriaTest {

    @Test
    void hasAdvancedFilters_WhenPartsTaxonomyIsSet_ShouldReturnTrue() {
        ProductFilterCriteria criteria = new ProductFilterCriteria();
        criteria.setPartsMainCategory("wheels-tires");

        assertThat(criteria.hasAdvancedFilters()).isTrue();
    }

    @Test
    void hasAdvancedFilters_WhenBranchSpecificWheelFilterIsSet_ShouldReturnTrue() {
        ProductFilterCriteria criteria = new ProductFilterCriteria();
        criteria.setWheelDiameterMin(new BigDecimal("18.0"));

        assertThat(criteria.hasAdvancedFilters()).isTrue();
    }

    @Test
    void hasAdvancedFilters_WhenBranchSpecificEngineFilterIsSet_ShouldReturnTrue() {
        ProductFilterCriteria criteria = new ProductFilterCriteria();
        criteria.setEngineType("i6");

        assertThat(criteria.hasAdvancedFilters()).isTrue();
    }

    @Test
    void hasAdvancedFilters_WhenNoFilterSet_ShouldReturnFalse() {
        ProductFilterCriteria criteria = new ProductFilterCriteria();

        assertThat(criteria.hasAdvancedFilters()).isFalse();
    }
}
