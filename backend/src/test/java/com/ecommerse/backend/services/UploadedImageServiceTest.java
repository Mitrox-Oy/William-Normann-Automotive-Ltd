package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.UploadedImage;
import com.ecommerse.backend.repositories.UploadedImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UploadedImageServiceTest {

    @Mock
    private UploadedImageRepository uploadedImageRepository;

    private UploadedImageService uploadedImageService;

    @BeforeEach
    void setUp() {
        uploadedImageService = new UploadedImageService(uploadedImageRepository);
    }

    @Test
    void saveShouldPersistImageBytesAndResolvedContentType() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "example.jpg",
                null,
                new byte[] {1, 2, 3});

        uploadedImageService.save("example.jpg", file);

        ArgumentCaptor<UploadedImage> captor = ArgumentCaptor.forClass(UploadedImage.class);
        verify(uploadedImageRepository).save(captor.capture());

        UploadedImage saved = captor.getValue();
        assertEquals("example.jpg", saved.getFileName());
        assertEquals("image/jpeg", saved.getContentType());
        assertArrayEquals(new byte[] {1, 2, 3}, saved.getImageData());
    }

    @Test
    void extractFileNameShouldHandlePathsAndNull() {
        assertEquals("image.png", uploadedImageService.extractFileName("products/image.png"));
        assertEquals("image.png", uploadedImageService.extractFileName("/api/images/image.png"));
        assertNull(uploadedImageService.extractFileName(null));
    }
}
