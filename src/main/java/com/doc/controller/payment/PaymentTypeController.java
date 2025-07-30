package com.doc.controller.payment;


import com.doc.dto.payment.PaymentTypeRequestDto;
import com.doc.dto.payment.PaymentTypeResponseDto;
import com.doc.service.PaymentTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment-types")
@Validated
public class PaymentTypeController {

    @Autowired
    private PaymentTypeService paymentTypeService;

    @PostMapping
    public ResponseEntity<PaymentTypeResponseDto> createPaymentType(@Valid @RequestBody PaymentTypeRequestDto requestDto) {
        PaymentTypeResponseDto response = paymentTypeService.createPaymentType(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentTypeResponseDto> getPaymentTypeById(@PathVariable Long id) {
        PaymentTypeResponseDto response = paymentTypeService.getPaymentTypeById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<PaymentTypeResponseDto>> getAllPaymentTypes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<PaymentTypeResponseDto> responses = paymentTypeService.getAllPaymentTypes(page, size);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentTypeResponseDto> updatePaymentType(
            @PathVariable Long id,
            @Valid @RequestBody PaymentTypeRequestDto requestDto) {
        PaymentTypeResponseDto response = paymentTypeService.updatePaymentType(id, requestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentType(@PathVariable Long id) {
        paymentTypeService.deletePaymentType(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
