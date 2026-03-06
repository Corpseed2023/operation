package com.doc.impl;

import com.doc.dto.company.CompanyRequestDto;
import com.doc.dto.company.CompanyResponseDto;
import com.doc.dto.company.unit.CompanyUnitRequestDto;
import com.doc.dto.company.unit.UnitSummary;
import com.doc.dto.contact.ContactRequestDto;
import com.doc.dto.contact.ContactSummary;
import com.doc.entity.client.Company;
import com.doc.entity.client.CompanyUnit;
import com.doc.entity.client.Contact;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.CompanyRepository;
import com.doc.repository.CompanyUnitRepository;
import com.doc.repository.ContactRepository;
import com.doc.repository.UserRepository;
import com.doc.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private static final Logger logger = LoggerFactory.getLogger(CompanyServiceImpl.class);

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyUnitRepository companyUnitRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CompanyResponseDto createCompany(CompanyRequestDto requestDto, Long externalCompanyId) {
        logger.info("Creating company: name={}, externalId={}, units={}, contacts={}",
                requestDto.getName(), externalCompanyId,
                requestDto.getUnits() != null ? requestDto.getUnits().size() : 0,
                requestDto.getContacts() != null ? requestDto.getContacts().size() : 0);

        // 1. Basic validation
        validateCompanyRequest(requestDto);

        // 2. Duplicate checks
        if (companyRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
            throw new ValidationException("Company name already exists: " + requestDto.getName(), "DUPLICATE_COMPANY_NAME");
        }
        if (StringUtils.hasText(requestDto.getPanNo()) &&
                companyRepository.existsByPanNoAndIsDeletedFalse(requestDto.getPanNo().trim())) {
            throw new ValidationException("PAN already exists: " + requestDto.getPanNo(), "DUPLICATE_PAN");
        }
        if (externalCompanyId != null && companyRepository.existsById(externalCompanyId)) {
            throw new ValidationException("External company ID already exists: " + externalCompanyId, "DUPLICATE_COMPANY_ID");
        }

        // 3. Validate creator exists
        User creator = userRepository.findById(requestDto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + requestDto.getCreatedBy(), "USER_NOT_FOUND"));

        // 4. Create Company entity
        Company company = new Company();
        if (externalCompanyId != null) {
            company.setId(externalCompanyId);
        }

        mapCompanyRequestToEntity(company, requestDto);
        company.setCreatedDate(new Date());
        company.setUpdatedDate(new Date());
        company.setDeleted(false);
        company.setCreatedById(creator.getId());
        company.setUpdatedBy(creator.getId());

        // Save company first (gets ID if auto-generated)
        company = companyRepository.save(company);
        logger.info("Company created: ID={}, Name={}", company.getId(), company.getName());

        // 5. Create Units (if provided)
        List<CompanyUnit> createdUnits = new ArrayList<>();
        if (requestDto.getUnits() != null && !requestDto.getUnits().isEmpty()) {
            for (CompanyUnitRequestDto unitDto : requestDto.getUnits()) {
                validateUnitRequest(unitDto);

                CompanyUnit unit = new CompanyUnit();
                mapUnitRequestToEntity(unit, unitDto);
                unit.setCompany(company);
                unit.setCreatedDate(new Date());
                unit.setUpdatedDate(new Date());
                unit.setCreatedBy(creator.getId());
                unit.setUpdatedBy(creator.getId());
                unit.setDeleted(false);
                unit.setStatus("Active");

                // Save unit
                unit = companyUnitRepository.save(unit);
                createdUnits.add(unit);
                company.getUnits().add(unit); // sync in-memory
            }
            logger.info("Created {} units for company {}", createdUnits.size(), company.getId());
        }

        // 6. Create Contacts (company-level or unit-level)
        List<Contact> createdContacts = new ArrayList<>();
        if (requestDto.getContacts() != null && !requestDto.getContacts().isEmpty()) {
            for (ContactRequestDto contactDto : requestDto.getContacts()) {
                validateContactRequest(contactDto);

                Contact contact = new Contact();
                mapContactRequestToEntity(contact, contactDto);

                // Assign level
                if (contactDto.getUnitId() != null) {
                    // Extract to final variables for lambda
                    final Long requestedUnitId = contactDto.getUnitId();
                    final Long companyId = company.getId();

                    CompanyUnit targetUnit = companyUnitRepository.findByIdAndCompanyIdAndIsDeletedFalse(
                                    requestedUnitId, companyId)
                            .orElseThrow(() -> new ValidationException(
                                    String.format("Unit ID %d not found or does not belong to company %d",
                                            requestedUnitId, companyId),
                                    "INVALID_UNIT_ID"));

                    if (Boolean.TRUE.equals(contactDto.getIsPrimary())) {
                        contact.assignAsPrimaryToUnit(targetUnit);
                    } else {
                        contact.assignAsSecondaryToUnit(targetUnit);
                    }
                } else {
                    // Default to company-level
                    if (Boolean.TRUE.equals(contactDto.getIsPrimary())) {
                        contact.assignAsPrimaryToCompany(company);
                    } else {
                        contact.assignAsSecondaryToCompany(company);
                    }
                }

                contact.setCreatedDate(new Date());
                contact.setUpdatedDate(new Date());
                contact.setCreatedBy(creator.getId());
                contact.setUpdatedBy(creator.getId());
                contact.setDeleteStatus(false);
                contact.setActive(true);

                // Save contact
                contact = contactRepository.save(contact);
                createdContacts.add(contact);
            }
            logger.info("Created {} contacts for company {}", createdContacts.size(), company.getId());
        }

        // 7. Final save (ensure all cascades are triggered)
        company = companyRepository.save(company);

        // 8. Build response
        return mapToResponseDto(company, createdUnits, createdContacts);
    }

    @Override
    public CompanyResponseDto getCompanyById(Long companyId) {

        logger.info("Fetching company with ID={}", companyId);

        // 1. Fetch company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found with id: " + companyId,
                        "COMPANY_NOT_FOUND"
                ));

        if (company.isDeleted()) {
            throw new ResourceNotFoundException(
                    "Company is deleted with id: " + companyId,
                    "COMPANY_DELETED"
            );
        }

        // 2. Fetch units
        List<CompanyUnit> units = companyUnitRepository
                .findByCompanyIdAndIsDeletedFalse(companyId);

        List<Long> unitIds = units.stream()
                .map(CompanyUnit::getId)
                .toList();

        // 4. Fetch contacts for those units
        List<Contact> contacts = new ArrayList<>();

        if (!unitIds.isEmpty()) {
            contacts.addAll(
                    contactRepository.findByCompanyUnitIds(unitIds)
            );
        }

        // 4. Map response
        return mapToResponseDto(company, units, contacts);
    }
    // ──────────────────────────────────────────────
    // Validation Helpers
    // ──────────────────────────────────────────────

    private void validateCompanyRequest(CompanyRequestDto dto) {
        if (StringUtils.isEmpty(dto.getName())) {
            throw new ValidationException("Company name is required", "INVALID_NAME");
        }
        if (dto.getCreatedBy() == null) {
            throw new ValidationException("CreatedBy user ID is required", "INVALID_CREATED_BY");
        }
    }

    private void validateUnitRequest(CompanyUnitRequestDto dto) {
        if (StringUtils.isEmpty(dto.getUnitName())) {
            throw new ValidationException("Unit name is required", "INVALID_UNIT_NAME");
        }
        if (StringUtils.isEmpty(dto.getAddress())) {
            throw new ValidationException("Unit address is required", "INVALID_UNIT_ADDRESS");
        }
    }

    private void validateContactRequest(ContactRequestDto dto) {
        if (StringUtils.isEmpty(dto.getName())) {
            throw new ValidationException("Contact name is required", "INVALID_CONTACT_NAME");
        }
    }

    // ──────────────────────────────────────────────
    // Mapping Helpers
    // ──────────────────────────────────────────────

    private void mapCompanyRequestToEntity(Company company, CompanyRequestDto dto) {
        company.setName(dto.getName().trim());
        company.setPanNo(dto.getPanNo() != null ? dto.getPanNo().trim() : null);
        company.setEstablishDate(dto.getEstablishDate());
        company.setIndustry(dto.getIndustry());
        company.setIndustries(dto.getIndustries());
        company.setSubIndustry(dto.getSubIndustry());
        company.setSubSubIndustry(dto.getSubSubIndustry());
    }

    private void mapUnitRequestToEntity(CompanyUnit unit, CompanyUnitRequestDto dto) {
        unit.setUnitName(dto.getUnitName().trim());
        unit.setId(dto.getUnitId());
        unit.setAddress(dto.getAddress().trim());
        unit.setCity(dto.getCity());
        unit.setState(dto.getState());
        unit.setCountry(dto.getCountry() != null ? dto.getCountry().trim() : "India");
        unit.setPinCode(dto.getPinCode());
        unit.setGstNo(dto.getGstNo() != null ? dto.getGstNo().trim() : null);
        unit.setGstType(dto.getGstType());
        unit.setGstBusinessType(dto.getGstBusinessType());
        unit.setUnitOpeningDate(dto.getUnitOpeningDate());
    }

    private void mapContactRequestToEntity(Contact contact, ContactRequestDto dto) {
        contact.setName(dto.getName().trim());
        contact.setTitle(dto.getTitle());
        contact.setDesignation(dto.getDesignation());
        contact.setEmail(dto.getEmail());
        contact.setId(dto.getContactId());
        contact.setContactNo(dto.getContactNo());
        contact.setWhatsappNo(dto.getWhatsappNo());
        contact.setClientDesignation(dto.getDesignation());
        // Add more mappings as per your DTO fields (remarks, department, etc.)
    }

    // ──────────────────────────────────────────────
    // Response Mapping
    // ──────────────────────────────────────────────

    private CompanyResponseDto mapToResponseDto(Company company, List<CompanyUnit> createdUnits, List<Contact> createdContacts) {
        CompanyResponseDto dto = new CompanyResponseDto();

        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setPanNo(company.getPanNo());
        dto.setEstablishDate(company.getEstablishDate());
        dto.setIndustry(company.getIndustry());
        dto.setSubIndustry(company.getSubIndustry());
        dto.setSubSubIndustry(company.getSubSubIndustry());
        dto.setCreatedDate(company.getCreatedDate());
        dto.setUpdatedDate(company.getUpdatedDate());
        dto.setDeleted(company.isDeleted());

        // Units summary
        dto.setUnitCount(createdUnits.size());
        List<UnitSummary> unitSummaries = new ArrayList<>();
        for (CompanyUnit unit : createdUnits) {
            UnitSummary summary = new UnitSummary();
            summary.setUnitId(unit.getId());
            summary.setUnitName(unit.getUnitName());
            summary.setCity(unit.getCity());
            summary.setGstNo(unit.getGstNo());
            unitSummaries.add(summary);
        }
        dto.setUnits(unitSummaries);

        // Contacts summary
        List<ContactSummary> contactSummaries = new ArrayList<>();

        // Unit-level contacts
        for (CompanyUnit unit : createdUnits) {
            List<Contact> unitContacts = unit.getUnitContacts();
            if (unitContacts != null) {
                for (Contact c : unitContacts) {
                    if (!c.isDeleted() && c.isActive()) {
                        ContactSummary summary = new ContactSummary();
                        summary.setContactId(c.getId());
                        summary.setName(c.getName());
                        summary.setDesignation(c.getDesignation() != null ? c.getDesignation() : c.getClientDesignation());
                        summary.setPrimary(c.isPrimaryForUnit());
                        summary.setSecondary(c.isSecondaryForUnit());
                        contactSummaries.add(summary);
                    }
                }
            }
        }

        // Company-level contacts
        for (Contact c : createdContacts) {
            if (c.getCompanyUnit() == null && !c.isDeleted() && c.isActive()) {
                ContactSummary summary = new ContactSummary();
                summary.setContactId(c.getId());
                summary.setName(c.getName());
                summary.setDesignation(c.getDesignation() != null ? c.getDesignation() : c.getClientDesignation());
                summary.setPrimary(c.isPrimaryForCompany());
                summary.setSecondary(c.isSecondaryForCompany());
                contactSummaries.add(summary);
            }
        }

        dto.setContactCount(contactSummaries.size());
        dto.setContacts(contactSummaries);

        return dto;
    }
}