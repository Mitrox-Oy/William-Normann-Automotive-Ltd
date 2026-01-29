package com.ecommerse.backend.repositories.analytics;

import com.ecommerse.backend.entities.analytics.FactCustomerMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FactCustomerMetricsRepository extends JpaRepository<FactCustomerMetrics, Long> {

    List<FactCustomerMetrics> findByCalculationDate(LocalDate date);

    List<FactCustomerMetrics> findByCohortMonth(String cohortMonth);

    @Query("SELECT AVG(f.lifetimeValue) FROM FactCustomerMetrics f WHERE f.calculationDate = :date")
    BigDecimal getAverageLifetimeValue(@Param("date") LocalDate date);
}
