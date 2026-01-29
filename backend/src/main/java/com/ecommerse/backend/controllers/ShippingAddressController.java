package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.ShippingAddressRequest;
import com.ecommerse.backend.dto.ShippingAddressResponse;
import com.ecommerse.backend.services.ShippingAddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/shipping")
public class ShippingAddressController {

    @Autowired
    private ShippingAddressService shippingAddressService;

    @GetMapping("/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ShippingAddressResponse>> getUserAddresses(Authentication authentication) {
        List<ShippingAddressResponse> addresses = shippingAddressService.getUserAddresses(authentication.getName());
        return ResponseEntity.ok(addresses);
    }

    @PostMapping("/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ShippingAddressResponse> addAddress(
            @Valid @RequestBody ShippingAddressRequest request,
            Authentication authentication) {
        ShippingAddressResponse address = shippingAddressService.addAddress(authentication.getName(), request);
        return ResponseEntity.ok(address);
    }

    @PutMapping("/addresses/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ShippingAddressResponse> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody ShippingAddressRequest request,
            Authentication authentication) {
        ShippingAddressResponse address = shippingAddressService.updateAddress(authentication.getName(), id, request);
        return ResponseEntity.ok(address);
    }

    @DeleteMapping("/addresses/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            Authentication authentication) {
        shippingAddressService.deleteAddress(authentication.getName(), id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/addresses/{id}/default")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ShippingAddressResponse> setDefaultAddress(
            @PathVariable Long id,
            Authentication authentication) {
        ShippingAddressResponse address = shippingAddressService.setDefaultAddress(authentication.getName(), id);
        return ResponseEntity.ok(address);
    }

    @GetMapping("/addresses/default")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ShippingAddressResponse> getDefaultAddress(Authentication authentication) {
        ShippingAddressResponse address = shippingAddressService.getDefaultAddress(authentication.getName());
        if (address != null) {
            return ResponseEntity.ok(address);
        } else {
            return ResponseEntity.noContent().build();
        }
    }
}
