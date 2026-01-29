package com.ecommerse.backend.services;

import com.ecommerse.backend.entities.OwnerWhitelist;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.OwnerWhitelistRepository;
import com.ecommerse.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhitelistManagementServiceTest {

    @Mock
    private OwnerWhitelistRepository whitelistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private WhitelistManagementService service;

    @BeforeEach
    void setUp() {
        service = new WhitelistManagementService();
        ReflectionTestUtils.setField(service, "whitelistRepository", whitelistRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
    }

    @Test
    void addToWhitelistUsesAdminProvidedPasswordWhenValid() {
        String email = "owner@example.com";
        String admin = "adminUser";
        String providedPassword = "AdminPass234!";

        when(whitelistRepository.existsByEmailAndIsActive(email, true)).thenReturn(false);
        when(whitelistRepository.save(any(OwnerWhitelist.class))).thenAnswer(invocation -> {
            OwnerWhitelist entry = invocation.getArgument(0);
            entry.setId(1L);
            return entry;
        });
        when(userRepository.findByUsername(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(providedPassword)).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WhitelistManagementService.WhitelistAddResult result = service.addToWhitelist(
            email,
            admin,
            "note",
            providedPassword
        );

        assertEquals(providedPassword, result.temporaryPassword());
        assertTrue(result.newUserCreated());

        verify(passwordEncoder).encode(providedPassword);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User createdUser = userCaptor.getValue();
        assertEquals("encoded", createdUser.getPassword());
        assertEquals(User.Role.OWNER, createdUser.getRole());
    }

    @Test
    void addToWhitelistAcceptsMixedCasePasswordWithDigitsAndSymbol() {
        String email = "mixed@example.com";
        String admin = "adminUser";
        String providedPassword = "TempPassword123!";

        when(whitelistRepository.existsByEmailAndIsActive(email, true)).thenReturn(false);
        when(whitelistRepository.save(any(OwnerWhitelist.class))).thenAnswer(invocation -> {
            OwnerWhitelist entry = invocation.getArgument(0);
            entry.setId(3L);
            return entry;
        });
        when(userRepository.findByUsername(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(providedPassword)).thenReturn("encoded-temp");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WhitelistManagementService.WhitelistAddResult result = service.addToWhitelist(
            email,
            admin,
            null,
            providedPassword
        );

        assertEquals(providedPassword, result.temporaryPassword());
        assertTrue(result.newUserCreated());
        verify(passwordEncoder).encode(providedPassword);
    }

    @Test
    void addToWhitelistRejectsInvalidAdminPassword() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            service.addToWhitelist(
                "owner@example.com",
                "admin",
                null,
                "short1"
            )
        );

        assertEquals("Temporary password must be at least 12 characters long", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void addToWhitelistGeneratesPasswordWhenNotProvided() {
        String email = "existing@example.com";
        User existingUser = new User(email, null, null, "old-encoded", User.Role.CUSTOMER);

        when(whitelistRepository.existsByEmailAndIsActive(email, true)).thenReturn(false);
        when(whitelistRepository.save(any(OwnerWhitelist.class))).thenAnswer(invocation -> {
            OwnerWhitelist entry = invocation.getArgument(0);
            entry.setId(2L);
            return entry;
        });
        when(userRepository.findByUsername(email)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        WhitelistManagementService.WhitelistAddResult result = service.addToWhitelist(
            email,
            "admin",
            null,
            null
        );

        assertNotNull(result.temporaryPassword());
        assertEquals(12, result.temporaryPassword().length());
        assertFalse(result.newUserCreated());

        verify(passwordEncoder).encode(result.temporaryPassword());
        verify(userRepository).save(existingUser);
        assertEquals("encoded-" + result.temporaryPassword(), existingUser.getPassword());
        assertEquals(User.Role.OWNER, existingUser.getRole());
    }
}
