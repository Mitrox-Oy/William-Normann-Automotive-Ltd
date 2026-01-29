package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.OwnerWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OwnerWhitelistRepository extends JpaRepository<OwnerWhitelist, Long> {

    @Query("SELECT ow FROM OwnerWhitelist ow WHERE ow.email = :email AND ow.isActive = :isActive")
    Optional<OwnerWhitelist> findByEmailAndIsActive(@Param("email") String email, @Param("isActive") boolean isActive);

    Optional<OwnerWhitelist> findByEmail(String email);

    @Query("SELECT COUNT(ow) > 0 FROM OwnerWhitelist ow WHERE ow.email = :email AND ow.isActive = :isActive")
    boolean existsByEmailAndIsActive(@Param("email") String email, @Param("isActive") boolean isActive);
}
