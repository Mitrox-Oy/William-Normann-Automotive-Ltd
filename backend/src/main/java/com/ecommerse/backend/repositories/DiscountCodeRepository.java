package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.DiscountCode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {

    @EntityGraph(attributePaths = { "categories" })
    Optional<DiscountCode> findByCodeIgnoreCase(String code);

    @EntityGraph(attributePaths = { "categories" })
    Optional<DiscountCode> findByCodeIgnoreCaseAndActiveTrue(String code);

    @EntityGraph(attributePaths = { "categories" })
    List<DiscountCode> findAllByOrderByCreatedDateDesc();
}
