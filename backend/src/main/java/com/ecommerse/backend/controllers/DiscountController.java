package com.ecommerse.backend.controllers;

import com.ecommerse.backend.dto.DiscountPreviewDTO;
import com.ecommerse.backend.dto.DiscountPreviewRequest;
import com.ecommerse.backend.services.DiscountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discounts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping("/preview")
    public ResponseEntity<DiscountPreviewDTO> previewDiscount(@Valid @RequestBody DiscountPreviewRequest request) {
        return ResponseEntity.ok(discountService.previewDiscount(request));
    }
}
