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
        // Log the full incoming request in detail
        logIncomingCompanyRequest(requestDto, externalCompanyId);

        logger.info("Creating company: name={}, externalId={}, units={}, contacts={}",
                safe(requestDto.getName()), externalCompanyId,
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

        List<CompanyUnit> units = companyUnitRepository
                .findByCompanyIdAndIsDeletedFalse(companyId);

        List<Long> unitIds = units.stream()
                .map(CompanyUnit::getId)
                .collect(Collectors.toList());

        List<Contact> contacts = new ArrayList<>();
        if (!unitIds.isEmpty()) {
            contacts.addAll(contactRepository.findByCompanyUnitIds(unitIds));
        }

        return mapToResponseDto(company, units, contacts);
    }

    // ──────────────────────────────────────────────
    // Logging helper – shows exactly what came in the request
    // ──────────────────────────────────────────────
    private void logIncomingCompanyRequest(CompanyRequestDto dto, Long externalId) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("\n")
                .append("═══════════════════════════════════════════════════════════════\n")
                .append("Incoming createCompany request\n")
                .append("═══════════════════════════════════════════════════════════════\n")
                .append("External company ID : ").append(externalId != null ? externalId : "(auto-generated)").append("\n")
                .append("Created by user ID   : ").append(dto.getCreatedBy()).append("\n")
                .append("───────────────────────────────────────────────────────────────\n")
                .append("Company:\n")
                .append("  • Name            : ").append(safe(dto.getName())).append("\n")
                .append("  • PAN             : ").append(safe(dto.getPanNo())).append("\n")
                .append("  • Industry        : ").append(safe(dto.getIndustry())).append("\n")
                .append("  • Sub Industry    : ").append(safe(dto.getSubIndustry())).append("\n")
                .append("  • Sub-Sub Industry: ").append(safe(dto.getSubSubIndustry())).append("\n")
                .append("───────────────────────────────────────────────────────────────\n");

        // Units
        int unitCount = dto.getUnits() != null ? dto.getUnits().size() : 0;
        sb.append("Units (").append(unitCount).append("):\n");
        if (unitCount > 0) {
            int i = 1;
            for (CompanyUnitRequestDto u : dto.getUnits()) {
                sb.append(String.format("  %2d. %-28s | %-18s | GST: %-15s | %s\n",
                        i++,
                        safe(u.getUnitName()),
                        safe(u.getCity()),
                        safe(u.getGstNo()),
                        safe(u.getAddress())));
            }
        } else {
            sb.append("  (no units provided)\n");
        }

        // Contacts
        int contactCount = dto.getContacts() != null ? dto.getContacts().size() : 0;
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("Contacts (").append(contactCount).append("):\n");
        if (contactCount > 0) {
            int i = 1;
            for (ContactRequestDto c : dto.getContacts()) {
                String scope = c.getUnitId() != null ? "unit:" + c.getUnitId() : "company";
                String role = Boolean.TRUE.equals(c.getIsPrimary()) ? "PRIMARY" :
                        Boolean.TRUE.equals(c.getIsSecondary()) ? "SECONDARY" : "-";
                sb.append(String.format("  %2d. %-22s | %-18s | %-25s | %-12s | %s - %s\n",
                        i++,
                        safe(c.getName()),
                        safe(c.getDesignation()),
                        safe(c.getEmail()),
                        safe(c.getContactNo()),
                        scope, role));
            }
        } else {
            sb.append("  (no contacts provided)\n");
        }

        sb.append("═══════════════════════════════════════════════════════════════\n");

        logger.info(sb.toString());
    }

    private String safe(String s) {
        if (s == null) return "(null)";
        String t = s.trim();
        return t.isEmpty() ? "(empty)" : t;
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
        company.setIndustry(dto.getIndustry());
        company.setIndustries(dto.getIndustries());
        company.setSubIndustry(dto.getSubIndustry());
        company.setSubSubIndustry(dto.getSubSubIndustry());
    }

    private void mapUnitRequestToEntity(CompanyUnit unit, CompanyUnitRequestDto dto) {
        unit.setUnitName(dto.getUnitName().trim());
        unit.setId(dto.getUnitId());   // ← note: setting external ID if provided
        unit.setAddress(dto.getAddress().trim());
        unit.setCity(dto.getCity());
        unit.setState(dto.getState());
        unit.setCountry(dto.getCountry() != null ? dto.getCountry().trim() : "India");
        unit.setPinCode(dto.getPinCode());
        unit.setGstNo(dto.getGstNo() != null ? dto.getGstNo().trim() : null);
        unit.setGstType(dto.getGstType());
        unit.setGstBusinessType(dto.getGstBusinessType());
    }

    private void mapContactRequestToEntity(Contact contact, ContactRequestDto dto) {
        contact.setName(dto.getName().trim());
        contact.setTitle(dto.getTitle());
        contact.setDesignation(dto.getDesignation());
        contact.setEmail(dto.getEmail());
        contact.setId(dto.getContactId());           // ← external ID if provided
        contact.setContactNo(dto.getContactNo());
        contact.setWhatsappNo(dto.getWhatsappNo());
        contact.setClientDesignation(dto.getDesignation());
        // Add more fields if your DTO has them (remarks, department, departmentId, etc.)
    }

    // ──────────────────────────────────────────────
    // Response Mapping
    // ──────────────────────────────────────────────

    private CompanyResponseDto mapToResponseDto(Company company, List<CompanyUnit> units, List<Contact> contacts) {
        CompanyResponseDto dto = new CompanyResponseDto();

        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setPanNo(company.getPanNo());
        dto.setIndustry(company.getIndustry());
        dto.setSubIndustry(company.getSubIndustry());
        dto.setSubSubIndustry(company.getSubSubIndustry());
        dto.setCreatedDate(company.getCreatedDate());
        dto.setUpdatedDate(company.getUpdatedDate());
        dto.setDeleted(company.isDeleted());

        // Units summary
        dto.setUnitCount(units.size());
        List<UnitSummary> unitSummaries = new ArrayList<>();
        for (CompanyUnit unit : units) {
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
        for (CompanyUnit unit : units) {
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

        // Company-level contacts (passed separately in create, but fetched from company in get)
        for (Contact c : contacts) {
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