package com.doc.impl;



import com.doc.dto.payment.PaymentTypeRequestDto;
import com.doc.dto.payment.PaymentTypeResponseDto;
import com.doc.entity.client.PaymentType;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;

import com.doc.repsoitory.PaymentTypeRepository;
import com.doc.repsoitory.UserRepository;
import com.doc.service.PaymentTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentTypeServiceImpl implements PaymentTypeService {

    @Autowired
    private PaymentTypeRepository paymentTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public PaymentTypeResponseDto createPaymentType(PaymentTypeRequestDto requestDto) {
        validateRequestDto(requestDto);

        if (paymentTypeRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
            throw new ValidationException("Payment type with name " + requestDto.getName() + " already exists");
        }

        userRepository.findActiveUserById(requestDto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getCreatedBy() + " not found"));

        PaymentType paymentType = new PaymentType();
        paymentType.setName(requestDto.getName().trim());
        paymentType.setCreatedDate(new Date());
        paymentType.setUpdatedDate(new Date());
        paymentType.setCreatedBy(requestDto.getCreatedBy());
        paymentType.setUpdatedBy(requestDto.getCreatedBy());
        paymentType.setDeleted(false);

        paymentType = paymentTypeRepository.save(paymentType);
        return mapToResponseDto(paymentType);
    }

    @Override
    public PaymentTypeResponseDto getPaymentTypeById(Long id) {
        PaymentType paymentType = paymentTypeRepository.findById(id)
                .filter(pt -> !pt.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Payment type with ID " + id + " not found"));
        return mapToResponseDto(paymentType);
    }

    @Override
    public List<PaymentTypeResponseDto> getAllPaymentTypes(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<PaymentType> paymentTypePage = paymentTypeRepository.findByIsDeletedFalse(pageable);
        return paymentTypePage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentTypeResponseDto updatePaymentType(Long id, PaymentTypeRequestDto requestDto) {
        validateRequestDto(requestDto);

        PaymentType paymentType = paymentTypeRepository.findById(id)
                .filter(pt -> !pt.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Payment type with ID " + id + " not found"));

        if (!paymentType.getName().equals(requestDto.getName().trim()) &&
                paymentTypeRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
            throw new ValidationException("Payment type with name " + requestDto.getName() + " already exists");
        }

        userRepository.findActiveUserById(requestDto.getUpdatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getUpdatedBy() + " not found"));

        paymentType.setName(requestDto.getName().trim());
        paymentType.setUpdatedDate(new Date());
        paymentType.setUpdatedBy(requestDto.getUpdatedBy());
        paymentType = paymentTypeRepository.save(paymentType);
        return mapToResponseDto(paymentType);
    }

    @Override
    public void deletePaymentType(Long id) {
        PaymentType paymentType = paymentTypeRepository.findById(id)
                .filter(pt -> !pt.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Payment type with ID " + id + " not found"));

        paymentType.setDeleted(true);
        paymentType.setUpdatedDate(new Date());
        paymentTypeRepository.save(paymentType);
    }

    private void validateRequestDto(PaymentTypeRequestDto requestDto) {
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new ValidationException("Payment type name cannot be empty");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null");
        }
        if (requestDto.getUpdatedBy() == null) {
            throw new ValidationException("Updated by user ID cannot be null");
        }
    }

    private PaymentTypeResponseDto mapToResponseDto(PaymentType paymentType) {
        PaymentTypeResponseDto dto = new PaymentTypeResponseDto();
        dto.setId(paymentType.getId());
        dto.setName(paymentType.getName());
        dto.setCreatedDate(paymentType.getCreatedDate());
        dto.setCreatedBy(paymentType.getCreatedBy());
        dto.setUpdatedDate(paymentType.getUpdatedDate());
        dto.setUpdatedBy(paymentType.getUpdatedBy());
        return dto;
    }
}
