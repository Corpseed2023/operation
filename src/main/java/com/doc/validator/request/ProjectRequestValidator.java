package com.doc.validator.request;

import com.doc.dto.project.ProjectRequestDto;
import com.doc.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProjectRequestValidator {

    private static final Logger logger = LoggerFactory.getLogger(ProjectRequestValidator.class);

    public void validate(ProjectRequestDto requestDto) {
        logger.debug("Validating project request DTO: {}", requestDto.getProjectNo());

        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            logger.warn("Project name is empty");
            throw new ValidationException("Project name cannot be empty");
        }
        if (requestDto.getProjectNo() == null || requestDto.getProjectNo().trim().isEmpty()) {
            logger.warn("Project number is empty");
            throw new ValidationException("Project number cannot be empty");
        }
        if (requestDto.getSalesPersonId() == null) {
            logger.warn("Sales person ID is null");
            throw new ValidationException("Sales person ID cannot be null");
        }
        if (requestDto.getTotalAmount() == null || requestDto.getTotalAmount() < 0) {
            logger.warn("Invalid total amount: {}", requestDto.getTotalAmount());
            throw new ValidationException("Total amount must be non-negative");
        }
        if (requestDto.getPaidAmount() == null || requestDto.getPaidAmount() < 0) {
            logger.warn("Invalid paid amount: {}", requestDto.getPaidAmount());
            throw new ValidationException("Paid amount must be non-negative");
        }
        if (requestDto.getPaidAmount() > requestDto.getTotalAmount()) {
            logger.warn("Paid amount {} exceeds total amount {}", requestDto.getPaidAmount(), requestDto.getTotalAmount());
            throw new ValidationException("Paid amount cannot exceed total amount");
        }
        if (!List.of("PENDING", "APPROVED", "REJECTED").contains(requestDto.getPaymentStatus())) {
            logger.warn("Invalid payment status: {}", requestDto.getPaymentStatus());
            throw new ValidationException("Payment status must be PENDING, APPROVED, or REJECTED");
        }
        if (requestDto.getProductId() == null) {
            logger.warn("Product ID is null");
            throw new ValidationException("Product ID cannot be null");
        }
        if (requestDto.getCompanyId() == null) {
            logger.warn("Company ID is null");
            throw new ValidationException("Company ID cannot be null");
        }
        if (requestDto.getContactId() == null) {
            logger.warn("Contact ID is null");
            throw new ValidationException("Contact ID cannot be null");
        }
        if (requestDto.getPaymentTypeId() == null) {
            logger.warn("Payment type ID is null");
            throw new ValidationException("Payment type ID cannot be null");
        }
        if (requestDto.getApprovedById() == null) {
            logger.warn("Approved by user ID is null");
            throw new ValidationException("Approved by user ID cannot be null");
        }
        if (requestDto.getCreatedBy() == null) {
            logger.warn("Created by user ID is null");
            throw new ValidationException("Created by user ID cannot be null");
        }
        if (requestDto.getUpdatedBy() == null) {
            logger.warn("Updated by user ID is null");
            throw new ValidationException("Updated by user ID cannot be null");
        }
        logger.debug("Project request DTO validated successfully");
    }
}