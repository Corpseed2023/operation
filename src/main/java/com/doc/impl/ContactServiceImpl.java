package com.doc.impl;

import com.doc.dto.contact.ContactRequestDto;
import com.doc.dto.contact.ContactResponseDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.CompanyUnit;
import com.doc.entity.client.Contact;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.CompanyRepository;
import com.doc.repository.CompanyUnitRepository;
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

import java.time.LocalDateTime;
import java.time.LocalDate;
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

    @Autowired
    private CompanyUnitRepository companyUnitRepository;


    @Override
    public List<ContactResponseDto> getAllContacts(int page, int size, Long companyId, Long userId) {


        return List.of();
    }

    @Override
    public ContactResponseDto updateContact(Long id, ContactRequestDto requestDto) {
        return null;
    }

    @Override
    public void deleteContact(Long id) {

    }



    @Override
    @jakarta.transaction.Transactional
    public ContactResponseDto createContact(ContactRequestDto dto) {

        // Validation
        if (dto.getCompanyId() == null && dto.getUnitId() == null) {
            throw new IllegalArgumentException("Contact must be linked to a Company or CompanyUnit");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Contact name is required");
        }

        Contact contact = new Contact();
        contact.setName(dto.getName().trim());
        contact.setTitle(dto.getTitle());
        contact.setEmail(dto.getEmail() != null ? dto.getEmail().trim() : null);
        contact.setContactNo(dto.getContactNo());
        contact.setWhatsappNo(dto.getWhatsappNo());
        contact.setDesignation(dto.getDesignation());
        contact.setUpdatedDate(new Date());
        contact.setDeleted(false);

        Company company = null;
        CompanyUnit unit = null;

        // Load Company
        if (dto.getCompanyId() != null) {
            company = companyRepository.findById(dto.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found: " + dto.getCompanyId()));
            contact.setCompany(company);
        }

        // Load Unit & inherit company if missing
        if (dto.getUnitId() != null) {
            unit = companyUnitRepository.findById(dto.getUnitId())
                    .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + dto.getUnitId()));
            contact.setCompanyUnit(unit);
            unit.getUnitContacts().add(contact);

            if (company == null) {
                company = unit.getCompany();
                if (company == null) {
                    throw new IllegalStateException("Unit has no parent Company");
                }
                contact.setCompany(company);
            }
        }

        // Save contact first (to get ID)
        contact = contactRepository.save(contact);

        // Save updated company/unit if roles changed
        if (company != null) {
            companyRepository.save(company);
        }
        if (unit != null) {
            companyUnitRepository.save(unit);
        }

        System.out.println("account service api called! ");


        return mapToResponseDto(contact);
    }

    @Override
    public ContactResponseDto getContactById(Long id) {
        return null;
    }

    private void validateRequestDto(ContactRequestDto requestDto) {
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new ValidationException("Contact name cannot be empty", "INVALID_CONTACT_NAME");
        }
        if (requestDto.getEmail() == null || requestDto.getEmail().trim().isEmpty()) {
            throw new ValidationException("Contact email cannot be empty", "INVALID_CONTACT_EMAIL");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null", "INVALID_CREATED_BY");
        }
        if (requestDto.getUpdatedBy() == null) {
            throw new ValidationException("Updated by user ID cannot be null", "INVALID_UPDATED_BY");
        }
        // Add additional validations as needed (e.g., email format, contact number format)
    }

    private void mapRequestDtoToEntity(Contact contact, ContactRequestDto requestDto, Company company) {
        contact.setTitle(requestDto.getTitle());
        contact.setName(requestDto.getName().trim());
        contact.setEmail(requestDto.getEmail().trim());
        contact.setContactNo(requestDto.getContactNo());
        contact.setWhatsappNo(requestDto.getWhatsappNo());
        contact.setCompany(company);
        // unit is need to be saved
        contact.setDesignation(requestDto.getDesignation());
        contact.setCreatedBy(requestDto.getCreatedBy());
        contact.setUpdatedBy(requestDto.getUpdatedBy());
    }

    private ContactResponseDto mapToResponseDto(Contact contact) {
        ContactResponseDto dto = new ContactResponseDto();
        dto.setId(contact.getId());
        dto.setTitle(contact.getTitle());
        dto.setName(contact.getName());
        dto.setEmails(contact.getEmail());
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