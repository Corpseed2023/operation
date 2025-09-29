package com.doc.validator.request;

import com.doc.dto.project.ProjectRequestDto;
import com.doc.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProjectRequestValidator {



    private static final Logger logger = LoggerFactory.getLogger(ProjectRequestValidator.class);

    public void validate(ProjectRequestDto requestDto) {
        logger.debug("Validating project request DTO: {}", requestDto.getProjectNo());

        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            logger.warn("Project name is empty");
            throw new ValidationException("Project name cannot be empty", "ERR_INVALID_PROJECT_NAME");
        }
        if (requestDto.getProjectNo() == null || requestDto.getProjectNo().trim().isEmpty()) {
            logger.warn("Project number is empty");
            throw new ValidationException("Project number cannot be empty", "ERR_INVALID_PROJECT_NO");
        }
        if (requestDto.getSalesPersonId() == null) {
            logger.warn("Sales person ID is null");
            throw new ValidationException("Sales person ID cannot be null", "ERR_NULL_SALES_PERSON");
        }
        if (requestDto.getTotalAmount() == null || requestDto.getTotalAmount() < 0) {
            logger.warn("Invalid total amount: {}", requestDto.getTotalAmount());
            throw new ValidationException("Total amount must be non-negative", "ERR_INVALID_TOTAL_AMOUNT");
        }
        if (requestDto.getPaidAmount() == null || requestDto.getPaidAmount() < 0) {
            logger.warn("Invalid paid amount: {}", requestDto.getPaidAmount());
            throw new ValidationException("Paid amount must be non-negative", "ERR_INVALID_PAID_AMOUNT");
        }
        if (requestDto.getPaidAmount() > requestDto.getTotalAmount()) {
            logger.warn("Paid amount {} exceeds total amount {}", requestDto.getPaidAmount(), requestDto.getTotalAmount());
            throw new ValidationException("Paid amount cannot exceed total amount", "ERR_EXCEEDS_TOTAL_AMOUNT");
        }

        if (requestDto.getProductId() == null) {

            logger.warn("Product ID is null");
            throw new ValidationException("Product ID cannot be null", "ERR_NULL_PRODUCT_ID");
        }
        if (requestDto.getCompanyId() == null) {
            logger.warn("Company ID is null");
            throw new ValidationException("Company ID cannot be null", "ERR_NULL_COMPANY_ID");
        }
        if (requestDto.getContactId() == null) {
            logger.warn("Contact ID is null");
            throw new ValidationException("Contact ID cannot be null", "ERR_NULL_CONTACT_ID");
        }
        if (requestDto.getPaymentTypeId() == null) {
            logger.warn("Payment type ID is null");
            throw new ValidationException("Payment type ID cannot be null", "ERR_NULL_PAYMENT_TYPE_ID");
        }
        if (requestDto.getApprovedById() == null) {
            logger.warn("Approved by user ID is null");
            throw new ValidationException("Approved by user ID cannot be null", "ERR_NULL_APPROVED_BY");
        }
        if (requestDto.getCreatedBy() == null) {
            logger.warn("Created by user ID is null");
            throw new ValidationException("Created by user ID cannot be null", "ERR_NULL_CREATED_BY");
        }
        if (requestDto.getUpdatedBy() == null) {
            logger.warn("Updated by user ID is null");
            throw new ValidationException("Updated by user ID cannot be null", "ERR_NULL_UPDATED_BY");
        }
        logger.debug("Project request DTO validated successfully");
    }
}