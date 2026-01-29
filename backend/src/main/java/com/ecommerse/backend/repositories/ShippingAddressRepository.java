package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.ShippingAddress;
import com.ecommerse.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {

    List<ShippingAddress> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);

    Optional<ShippingAddress> findByUserAndIsDefaultTrue(User user);

    @Modifying
    @Query("UPDATE ShippingAddress sa SET sa.isDefault = false WHERE sa.user = :user AND sa.id != :excludeId")
    void setAllNonDefaultForUser(@Param("user") User user, @Param("excludeId") Long excludeId);

    @Modifying
    @Query("UPDATE ShippingAddress sa SET sa.isDefault = false WHERE sa.user = :user")
    void setAllNonDefaultForUser(@Param("user") User user);

    Optional<ShippingAddress> findByIdAndUser(Long id, User user);

    void deleteByIdAndUser(Long id, User user);

    long countByUser(User user);
}

