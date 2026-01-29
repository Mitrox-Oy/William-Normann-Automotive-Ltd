package com.ecommerse.backend.services;

import com.ecommerse.backend.dto.ShippingAddressRequest;
import com.ecommerse.backend.dto.ShippingAddressResponse;
import com.ecommerse.backend.entities.ShippingAddress;
import com.ecommerse.backend.entities.User;
import com.ecommerse.backend.repositories.ShippingAddressRepository;
import com.ecommerse.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShippingAddressService {

    @Autowired
    private ShippingAddressRepository shippingAddressRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ShippingAddressResponse> getUserAddresses(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ShippingAddress> addresses = shippingAddressRepository
                .findByUserOrderByIsDefaultDescCreatedAtDesc(user);

        return addresses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShippingAddressResponse addAddress(String username, ShippingAddressRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ShippingAddressRequest sanitized = sanitizeRequest(request);
        boolean hasExistingAddresses = shippingAddressRepository.countByUser(user) > 0;
        boolean setAsDefault = sanitized.isDefault() || !hasExistingAddresses;

        if (setAsDefault) {
            shippingAddressRepository.setAllNonDefaultForUser(user);
        }

        ShippingAddress address = new ShippingAddress(
                user,
                normalizeRequired(sanitized.getFullName()),
                normalizeRequired(sanitized.getAddressLine1()),
                normalizeRequired(sanitized.getCity()),
                normalizeRequired(sanitized.getState()),
                normalizeRequired(sanitized.getPostalCode()),
                normalizeRequired(sanitized.getCountry()));

        address.setAddressLine2(trimToNull(sanitized.getAddressLine2()));
        address.setPhoneNumber(trimToNull(sanitized.getPhoneNumber()));
        address.setDefault(setAsDefault);

        ShippingAddress savedAddress = shippingAddressRepository.save(address);
        return convertToResponse(savedAddress);
    }

    @Transactional
    public ShippingAddressResponse updateAddress(String username, Long addressId, ShippingAddressRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ShippingAddress address = shippingAddressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        ShippingAddressRequest sanitized = sanitizeRequest(request);

        if (sanitized.isDefault()) {
            shippingAddressRepository.setAllNonDefaultForUser(user, addressId);
        }

        address.setFullName(normalizeRequired(sanitized.getFullName()));
        address.setAddressLine1(normalizeRequired(sanitized.getAddressLine1()));
        address.setAddressLine2(trimToNull(sanitized.getAddressLine2()));
        address.setCity(normalizeRequired(sanitized.getCity()));
        address.setState(normalizeRequired(sanitized.getState()));
        address.setPostalCode(normalizeRequired(sanitized.getPostalCode()));
        address.setCountry(normalizeRequired(sanitized.getCountry()));
        address.setPhoneNumber(trimToNull(sanitized.getPhoneNumber()));

        boolean keepDefault = sanitized.isDefault();
        if (!keepDefault && address.isDefault()) {
            long addressCount = shippingAddressRepository.countByUser(user);
            keepDefault = addressCount <= 1;
        }
        address.setDefault(keepDefault);

        ShippingAddress updatedAddress = shippingAddressRepository.save(address);
        return convertToResponse(updatedAddress);
    }

    @Transactional
    public void deleteAddress(String username, Long addressId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ShippingAddress address = shippingAddressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        shippingAddressRepository.delete(address);
    }

    @Transactional
    public ShippingAddressResponse setDefaultAddress(String username, Long addressId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ShippingAddress address = shippingAddressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        // Make all other addresses non-default
        shippingAddressRepository.setAllNonDefaultForUser(user, addressId);

        // Set this address as default
        address.setDefault(true);
        ShippingAddress updatedAddress = shippingAddressRepository.save(address);

        return convertToResponse(updatedAddress);
    }

    public ShippingAddressResponse getDefaultAddress(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ShippingAddress defaultAddress = shippingAddressRepository.findByUserAndIsDefaultTrue(user)
                .orElse(null);

        return defaultAddress != null ? convertToResponse(defaultAddress) : null;
    }

    private ShippingAddressRequest sanitizeRequest(ShippingAddressRequest request) {
        request.setFullName(normalizeRequired(request.getFullName()));
        request.setAddressLine1(normalizeRequired(request.getAddressLine1()));
        request.setAddressLine2(trimToNull(request.getAddressLine2()));
        request.setCity(normalizeRequired(request.getCity()));
        request.setState(normalizeRequired(request.getState()));
        request.setPostalCode(normalizeRequired(request.getPostalCode()));
        request.setCountry(normalizeRequired(request.getCountry()));
        request.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        return request;
    }

    private String normalizeRequired(String value) {
        return value != null ? value.trim() : null;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private ShippingAddressResponse convertToResponse(ShippingAddress address) {
        return new ShippingAddressResponse(
                address.getId(),
                address.getFullName(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getPhoneNumber(),
                address.isDefault());
    }
}




