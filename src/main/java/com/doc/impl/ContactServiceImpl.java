package com.doc.impl;

import com.doc.dto.contact.ContactRequestDto;
import com.doc.dto.contact.ContactResponseDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.Contact;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;

import com.doc.repository.CompanyRepository;
import com.doc.repository.ContactRepository;
import com.doc.repository.UserRepository;
import com.doc.service.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ContactServiceImpl implements ContactService {

    private static final Logger logger = LoggerFactory.getLogger(ContactServiceImpl.class);

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ContactResponseDto createContact(ContactRequestDto requestDto) {
        logger.info("Creating contact with name: {}, email: {}, companyId: {}",
                requestDto.getName(), requestDto.getEmails(), requestDto.getCompanyId());
        validateRequestDto(requestDto);

        // Check for duplicate email within the company (or globally if companyId is null)
        Long companyId = requestDto.getCompanyId();
        if (contactRepository.existsByEmailsAndCompanyIdAndDeleteStatusFalse(requestDto.getEmails(), companyId)) {
            throw new ValidationException("Contact with email " + requestDto.getEmails() + " already exists for the company");
        }

        Company company = null;
        if (requestDto.getCompanyId() != null) {
            company = companyRepository.findById(requestDto.getCompanyId())
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Company with ID " + requestDto.getCompanyId() + " not found"));
        }

        userRepository.findActiveUserById(requestDto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getCreatedBy() + " not found"));

        Contact contact = new Contact();
        mapRequestDtoToEntity(contact, requestDto, company);
        contact.setCreatedDate(new Date());
        contact.setUpdatedDate(new Date());
        contact.setUpdatedBy(requestDto.getCreatedBy());
        contact.setDeleteStatus(false);

        contact = contactRepository.save(contact);
        return mapToResponseDto(contact);
    }

    @Override
    public ContactResponseDto getContactById(Long id) {
        logger.info("Fetching contact with ID: {}", id);
        Contact contact = contactRepository.findById(id)
                .filter(c -> !c.isDeleteStatus())
                .orElseThrow(() -> new ResourceNotFoundException("Contact with ID " + id + " not found"));
        return mapToResponseDto(contact);
    }

    @Override
    public List<ContactResponseDto> getAllContacts(int page, int size) {
        logger.info("Fetching contacts, page: {}, size: {}", page, size);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Contact> contactPage = contactRepository.findByDeleteStatusFalse(pageable);
        return contactPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public ContactResponseDto updateContact(Long id, ContactRequestDto requestDto) {
        logger.info("Updating contact with ID: {}, email: {}, companyId: {}",
                id, requestDto.getEmails(), requestDto.getCompanyId());
        validateRequestDto(requestDto);

        Contact contact = contactRepository.findById(id)
                .filter(c -> !c.isDeleteStatus())
                .orElseThrow(() -> new ResourceNotFoundException("Contact with ID " + id + " not found"));

        // Check for duplicate email within the company (or globally if companyId is null)
        Long companyId = requestDto.getCompanyId();
        if (!contact.getEmails().equals(requestDto.getEmails()) &&
                contactRepository.existsByEmailsAndCompanyIdAndDeleteStatusFalse(requestDto.getEmails(), companyId)) {
            throw new ValidationException("Contact with email " + requestDto.getEmails() + " already exists for the company");
        }

        Company company = null;
        if (requestDto.getCompanyId() != null) {
            company = companyRepository.findById(requestDto.getCompanyId())
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> new ResourceNotFoundException("Company with ID " + requestDto.getCompanyId() + " not found"));
        }

        userRepository.findActiveUserById(requestDto.getUpdatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getUpdatedBy() + " not found"));

        mapRequestDtoToEntity(contact, requestDto, company);
        contact.setUpdatedDate(new Date());
        contact = contactRepository.save(contact);
        return mapToResponseDto(contact);
    }

    @Override
    public void deleteContact(Long id) {
        logger.info("Deleting contact with ID: {}", id);
        Contact contact = contactRepository.findById(id)
                .filter(c -> !c.isDeleteStatus())
                .orElseThrow(() -> new ResourceNotFoundException("Contact with ID " + id + " not found"));

        contact.setDeleteStatus(true);
        contact.setUpdatedDate(new Date());
        contactRepository.save(contact);
    }

    private void validateRequestDto(ContactRequestDto requestDto) {
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new ValidationException("Contact name cannot be empty");
        }
        if (requestDto.getEmails() == null || requestDto.getEmails().trim().isEmpty()) {
            throw new ValidationException("Contact email cannot be empty");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null");
        }
        if (requestDto.getUpdatedBy() == null) {
            throw new ValidationException("Updated by user ID cannot be null");
        }
        // Add additional validations as needed (e.g., email format, contact number format)
    }

    private void mapRequestDtoToEntity(Contact contact, ContactRequestDto requestDto, Company company) {
        contact.setTitle(requestDto.getTitle());
        contact.setName(requestDto.getName().trim());
        contact.setEmails(requestDto.getEmails().trim());
        contact.setContactNo(requestDto.getContactNo());
        contact.setWhatsappNo(requestDto.getWhatsappNo());
        contact.setCompany(company);
        contact.setDesignation(requestDto.getDesignation());
        contact.setCreatedBy(requestDto.getCreatedBy());
        contact.setUpdatedBy(requestDto.getUpdatedBy());
    }

    private ContactResponseDto mapToResponseDto(Contact contact) {
        ContactResponseDto dto = new ContactResponseDto();
        dto.setId(contact.getId());
        dto.setTitle(contact.getTitle());
        dto.setName(contact.getName());
        dto.setEmails(contact.getEmails());
        dto.setContactNo(contact.getContactNo());
        dto.setWhatsappNo(contact.getWhatsappNo());
        dto.setCompanyId(contact.getCompany() != null ? contact.getCompany().getId() : null);
        dto.setDesignation(contact.getDesignation());
        dto.setCreatedDate(contact.getCreatedDate());
        dto.setCreatedBy(contact.getCreatedBy());
        dto.setUpdatedDate(contact.getUpdatedDate());
        dto.setUpdatedBy(contact.getUpdatedBy());
        return dto;
    }
}