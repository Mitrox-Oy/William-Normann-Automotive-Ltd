package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.DiscountCodeDTO;
import com.ecommerse.backend.services.DiscountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/discounts")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class AdminDiscountController {

    private final DiscountService discountService;

    public AdminDiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping
    public ResponseEntity<List<DiscountCodeDTO>> getAllDiscounts() {
        return ResponseEntity.ok(discountService.getAllDiscountCodes());
    }

    @PostMapping
    public ResponseEntity<DiscountCodeDTO> createDiscount(@Valid @RequestBody DiscountCodeDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discountService.createDiscountCode(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiscountCodeDTO> updateDiscount(@PathVariable Long id,
            @Valid @RequestBody DiscountCodeDTO request) {
        return ResponseEntity.ok(discountService.updateDiscountCode(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiscount(@PathVariable Long id) {
        discountService.deleteDiscountCode(id);
        return ResponseEntity.noContent().build();
    }
}
