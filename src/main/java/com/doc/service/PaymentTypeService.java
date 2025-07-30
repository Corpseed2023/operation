package com.doc.service;


import com.doc.dto.payment.PaymentTypeRequestDto;
import com.doc.dto.payment.PaymentTypeResponseDto;

import java.util.List;

public interface PaymentTypeService {

    PaymentTypeResponseDto createPaymentType(PaymentTypeRequestDto requestDto);

    PaymentTypeResponseDto getPaymentTypeById(Long id);

    List<PaymentTypeResponseDto> getAllPaymentTypes(int page, int size);

    PaymentTypeResponseDto updatePaymentType(Long id, PaymentTypeRequestDto requestDto);

    void deletePaymentType(Long id);
}
