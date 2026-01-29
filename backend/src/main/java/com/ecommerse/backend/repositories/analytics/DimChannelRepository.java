package com.ecommerse.backend.repositories.analytics;

import com.ecommerse.backend.entities.analytics.DimChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DimChannelRepository extends JpaRepository<DimChannel, Long> {

    Optional<DimChannel> findByChannelCode(String channelCode);
}
