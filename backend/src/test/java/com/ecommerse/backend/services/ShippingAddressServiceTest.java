package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ShippingAddressRequest;
import com.ecommerse.backend.dto.ShippingAddressResponse;
import com.ecommerse.backend.entities.ShippingAddress;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.ShippingAddressRepository;
import com.ecommerse.backend.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingAddressServiceTest {

    private static final String USERNAME = "customer@example.com";

    @Mock
    private ShippingAddressRepository shippingAddressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ShippingAddressService shippingAddressService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(12L);
        user.setUsername(USERNAME);
    }

    @Test
    void addAddressSetsDefaultWhenUserHasNoAddresses() {
        stubUserLookup();
        ShippingAddressRequest request = buildRequest();
        when(shippingAddressRepository.countByUser(user)).thenReturn(0L);

        ShippingAddress persisted = new ShippingAddress(user, "Jane Doe", "123 Main St", "Austin", "TX", "78701",
                "USA");
        persisted.setId(44L);
        persisted.setDefault(true);
        when(shippingAddressRepository.save(any(ShippingAddress.class))).thenReturn(persisted);

        ShippingAddressResponse response = shippingAddressService.addAddress(USERNAME, request);

        assertThat(response.isDefault()).isTrue();
        ArgumentCaptor<ShippingAddress> addressCaptor = ArgumentCaptor.forClass(ShippingAddress.class);
        verify(shippingAddressRepository).save(addressCaptor.capture());
        ShippingAddress saved = addressCaptor.getValue();
        assertThat(saved.isDefault()).isTrue();
        assertThat(saved.getPhoneNumber()).isEqualTo("5125551212");
        verify(shippingAddressRepository).setAllNonDefaultForUser(user);
    }

    @Test
    void addAddressDoesNotSetDefaultWhenUserHasExistingAddresses() {
        stubUserLookup();
        ShippingAddressRequest request = buildRequest();
        request.setDefault(false);
        when(shippingAddressRepository.countByUser(user)).thenReturn(2L);

        ShippingAddress persisted = new ShippingAddress(user, "Jane Doe", "123 Main St", "Austin", "TX", "78701",
                "USA");
        persisted.setId(45L);
        persisted.setDefault(false);
        when(shippingAddressRepository.save(any(ShippingAddress.class))).thenReturn(persisted);

        ShippingAddressResponse response = shippingAddressService.addAddress(USERNAME, request);

        assertThat(response.isDefault()).isFalse();
        verify(shippingAddressRepository, never()).setAllNonDefaultForUser(user);
    }

    @Test
    void addAddressTrimsOptionalFieldsToNull() {
        stubUserLookup();
        ShippingAddressRequest request = buildRequest();
        request.setAddressLine2("   ");
        request.setPhoneNumber("  ");
        when(shippingAddressRepository.countByUser(user)).thenReturn(1L);

        ShippingAddress persisted = new ShippingAddress(user, "Jane Doe", "123 Main St", "Austin", "TX", "78701",
                "USA");
        persisted.setId(46L);
        when(shippingAddressRepository.save(any(ShippingAddress.class))).thenReturn(persisted);

        shippingAddressService.addAddress(USERNAME, request);

        ArgumentCaptor<ShippingAddress> addressCaptor = ArgumentCaptor.forClass(ShippingAddress.class);
        verify(shippingAddressRepository).save(addressCaptor.capture());
        ShippingAddress saved = addressCaptor.getValue();
        assertThat(saved.getAddressLine2()).isNull();
        assertThat(saved.getPhoneNumber()).isNull();
    }

    @Test
    void updateAddressSetsDefaultWhenRequested() {
        stubUserLookup();
        ShippingAddress existing = new ShippingAddress(user, "Jane Doe", "123 Main St", "Austin", "TX", "78701",
                "USA");
        existing.setId(47L);
        when(shippingAddressRepository.findByIdAndUser(47L, user)).thenReturn(Optional.of(existing));

        ShippingAddressRequest request = buildRequest();
        request.setDefault(true);

        when(shippingAddressRepository.save(existing)).thenReturn(existing);

        ShippingAddressResponse response = shippingAddressService.updateAddress(USERNAME, 47L, request);

        assertThat(response.isDefault()).isTrue();
        verify(shippingAddressRepository).setAllNonDefaultForUser(user, 47L);
        assertThat(existing.getAddressLine1()).isEqualTo("123 Main St");
        assertThat(existing.getCity()).isEqualTo("Austin");
    }

    @Test
    void updateAddressKeepsExistingDefaultWhenOnlyAddress() {
        stubUserLookup();
        ShippingAddress existing = new ShippingAddress(user, "Jane Doe", "123 Main St", "Austin", "TX", "78701",
                "USA");
        existing.setId(48L);
        existing.setDefault(true);
        when(shippingAddressRepository.findByIdAndUser(48L, user)).thenReturn(Optional.of(existing));
        when(shippingAddressRepository.countByUser(user)).thenReturn(1L);

        ShippingAddressRequest request = buildRequest();
        request.setDefault(false);
        when(shippingAddressRepository.save(existing)).thenReturn(existing);

        ShippingAddressResponse response = shippingAddressService.updateAddress(USERNAME, 48L, request);

        assertThat(response.isDefault()).isTrue();
        verify(shippingAddressRepository, never()).setAllNonDefaultForUser(user, 48L);
    }

    @Test
    void deleteAddressRemovesEntity() {
        stubUserLookup();
        ShippingAddress existing = new ShippingAddress(user, "Jane Doe", "123 Main St", "Austin", "TX", "78701",
                "USA");
        existing.setId(49L);
        when(shippingAddressRepository.findByIdAndUser(49L, user)).thenReturn(Optional.of(existing));

        shippingAddressService.deleteAddress(USERNAME, 49L);

        verify(shippingAddressRepository).delete(existing);
    }

    @Test
    void setDefaultAddressClearsExistingDefaults() {
        stubUserLookup();
        ShippingAddress existing = new ShippingAddress(user, "Jane Doe", "123 Main St", "Austin", "TX", "78701",
                "USA");
        existing.setId(50L);
        when(shippingAddressRepository.findByIdAndUser(50L, user)).thenReturn(Optional.of(existing));
        when(shippingAddressRepository.save(existing)).thenReturn(existing);

        ShippingAddressResponse response = shippingAddressService.setDefaultAddress(USERNAME, 50L);

        assertThat(response.isDefault()).isTrue();
        verify(shippingAddressRepository).setAllNonDefaultForUser(user, 50L);
        verify(shippingAddressRepository).save(existing);
    }

    @Test
    void getDefaultAddressReturnsNullWhenUnset() {
        stubUserLookup();
        when(shippingAddressRepository.findByUserAndIsDefaultTrue(user)).thenReturn(Optional.empty());

        ShippingAddressResponse response = shippingAddressService.getDefaultAddress(USERNAME);

        assertThat(response).isNull();
    }

    @Test
    void getUserAddressesMapsEntitiesToResponses() {
        stubUserLookup();
        ShippingAddress a = new ShippingAddress(user, "Jane Doe", "123 Main St", "Austin", "TX", "78701", "USA");
        a.setId(60L);
        a.setDefault(true);
        ShippingAddress b = new ShippingAddress(user, "Alex Smith", "9 Elm St", "Dallas", "TX", "75001", "USA");
        b.setId(61L);
        when(shippingAddressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user))
                .thenReturn(List.of(a, b));

        List<ShippingAddressResponse> responses = shippingAddressService.getUserAddresses(USERNAME);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).isDefault()).isTrue();
        assertThat(responses.get(1).getFullName()).isEqualTo("Alex Smith");
    }

    @Test
    void throwsWhenUserNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shippingAddressService.getUserAddresses("missing"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    private void stubUserLookup() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    private ShippingAddressRequest buildRequest() {
        ShippingAddressRequest request = new ShippingAddressRequest();
        request.setFullName(" Jane Doe ");
        request.setAddressLine1(" 123 Main St ");
        request.setCity(" Austin ");
        request.setState(" TX ");
        request.setPostalCode("78701");
        request.setCountry("USA ");
        request.setPhoneNumber("5125551212");
        request.setDefault(true);
        return request;
    }
}
