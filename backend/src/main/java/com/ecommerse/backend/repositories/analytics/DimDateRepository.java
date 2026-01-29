package com.ecommerse.backend.repositories.analytics;

import com.ecommerse.backend.entities.analytics.DimDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DimDateRepository extends JpaRepository<DimDate, Integer> {

    Optional<DimDate> findByDate(LocalDate date);
}
