package com.ecommerse.backend.repositories;

import com.ecommerse.backend.entities.UploadedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadedImageRepository extends JpaRepository<UploadedImage, String> {
}
