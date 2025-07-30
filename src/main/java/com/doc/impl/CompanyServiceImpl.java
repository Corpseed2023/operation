package com.doc.impl;

import com.doc.dto.company.CompanyRequestDto;
import com.doc.dto.company.CompanyResponseDto;
import com.doc.dto.contact.ContactRequestDto;
import com.doc.entity.client.Company;
import com.doc.entity.client.Contact;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;

import com.doc.repsoitory.CompanyRepository;
import com.doc.repsoitory.ContactRepository;
import com.doc.repsoitory.UserRepository;
import com.doc.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private ContactRepository contactRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CompanyResponseDto createCompany(CompanyRequestDto requestDto) {
        logger.info("Creating company with name: {}, number of contacts: {}",
                requestDto.getName(), requestDto.getContacts() != null ? requestDto.getContacts().size() : 0);
        validateRequestDto(requestDto);

        if (companyRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
            throw new ValidationException("Company with name " + requestDto.getName() + " already exists");
        }

        if (requestDto.getGstNo() != null && !requestDto.getGstNo().isEmpty() &&
                companyRepository.existsByGstNoAndIsDeletedFalse(requestDto.getGstNo())) {
            throw new ValidationException("Company with GSTIN " + requestDto.getGstNo() + " already exists");
        }

        userRepository.findActiveUserById(requestDto.getCreatedBy())
                .orElseThrow(() -> new ResourceNotFoundException("Active user with ID " + requestDto.getCreatedBy() + " not found"));

        Company company = new Company();
        mapRequestDtoToEntity(company, requestDto);
        company.setCreatedDate(new Date());
        company.setUpdatedDate(new Date());
        company.setDeleted(false);

        // Map and associate contacts
        List<Contact> contacts = mapContactDtosToEntities(requestDto.getContacts(), company);
        company.setContacts(contacts);

        company = companyRepository.save(company);
        return mapToResponseDto(company);
    }

    @Override
    public CompanyResponseDto getCompanyById(Long id) {
        logger.info("Fetching company with ID: {}", id);
        Company company = companyRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Company with ID " + id + " not found"));
        return mapToResponseDto(company);
    }

    @Override
    public List<CompanyResponseDto> getAllCompanies(int page, int size) {
        logger.info("Fetching companies, page: {}, size: {}", page, size);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Company> companyPage = companyRepository.findByIsDeletedFalse(pageable);
        return companyPage.getContent()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public CompanyResponseDto updateCompany(Long id, CompanyRequestDto requestDto) {
        logger.info("Updating company with ID: {}, number of contacts: {}",
                id, requestDto.getContacts() != null ? requestDto.getContacts().size() : 0);
        validateRequestDto(requestDto);

        Company company = companyRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Company with ID " + id + " not found"));

        if (!company.getName().equals(requestDto.getName().trim()) &&
                companyRepository.existsByNameAndIsDeletedFalse(requestDto.getName().trim())) {
            throw new ValidationException("Company with name " + requestDto.getName() + " already exists");
        }

        if (requestDto.getGstNo() != null && !requestDto.getGstNo().isEmpty() &&
                !company.getGstNo().equals(requestDto.getGstNo()) &&
                companyRepository.existsByGstNoAndIsDeletedFalse(requestDto.getGstNo())) {
            throw new ValidationException("Company with GSTIN " + requestDto.getGstNo() + " already exists");
        }

        // Update company fields
        mapRequestDtoToEntity(company, requestDto);
        company.setUpdatedDate(new Date());

        // Update contacts
        company.getContacts().clear(); // Remove existing contacts
        List<Contact> contacts = mapContactDtosToEntities(requestDto.getContacts(), company);
        company.setContacts(contacts);

        company = companyRepository.save(company);
        return mapToResponseDto(company);
    }

    @Override
    public void deleteCompany(Long id) {
        logger.info("Deleting company with ID: {}", id);
        Company company = companyRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Company with ID " + id + " not found"));

        company.setDeleted(true);
        company.setUpdatedDate(new Date());
        companyRepository.save(company);
    }

    private void validateRequestDto(CompanyRequestDto requestDto) {
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new ValidationException("Company name cannot be empty");
        }
        if (requestDto.getCreatedBy() == null) {
            throw new ValidationException("Created by user ID cannot be null");
        }
        if (requestDto.getContacts() != null) {
            for (ContactRequestDto contactDto : requestDto.getContacts()) {
                if (contactDto.getName() == null || contactDto.getName().trim().isEmpty()) {
                    throw new ValidationException("Contact name cannot be empty");
                }
                if (contactDto.getEmails() == null || contactDto.getEmails().trim().isEmpty()) {
                    throw new ValidationException("Contact email cannot be empty");
                }
                if (contactDto.getCreatedBy() == null) {
                    throw new ValidationException("Contact created by user ID cannot be null");
                }
                if (contactDto.getUpdatedBy() == null) {
                    throw new ValidationException("Contact updated by user ID cannot be null");
                }
                if (contactRepository.existsByEmailsAndCompanyIdAndDeleteStatusFalse(contactDto.getEmails(), null)) {
                    throw new ValidationException("Contact with email " + contactDto.getEmails() + " already exists");
                }
            }
        }
    }

    private void mapRequestDtoToEntity(Company company, CompanyRequestDto requestDto) {
        company.setName(requestDto.getName().trim());
        company.setCompanyGstType(requestDto.getCompanyGstType());
        company.setGstBusinessType(requestDto.getGstBusinessType());
        company.setGstNo(requestDto.getGstNo());
        company.setEstablishDate(requestDto.getEstablishDate());
        company.setIndustry(requestDto.getIndustry());
        company.setAddress(requestDto.getAddress());
        company.setCity(requestDto.getCity());
        company.setState(requestDto.getState());
        company.setCountry(requestDto.getCountry());
        company.setPrimaryPinCode(requestDto.getPrimaryPinCode());
        company.setIndustries(requestDto.getIndustries());
        company.setSubIndustry(requestDto.getSubIndustry());
        company.setSubSubIndustry(requestDto.getSubSubIndustry());
    }

    private List<Contact> mapContactDtosToEntities(List<ContactRequestDto> contactDtos, Company company) {
        List<Contact> contacts = new ArrayList<>();
        if (contactDtos != null) {
            for (ContactRequestDto dto : contactDtos) {
                Contact contact = new Contact();
                contact.setTitle(dto.getTitle());
                contact.setName(dto.getName().trim());
                contact.setEmails(dto.getEmails().trim());
                contact.setContactNo(dto.getContactNo());
                contact.setWhatsappNo(dto.getWhatsappNo());
                contact.setDesignation(dto.getDesignation());
                contact.setCompany(company);
                contact.setCreatedBy(dto.getCreatedBy());
                contact.setUpdatedBy(dto.getUpdatedBy());
                contact.setCreatedDate(new Date());
                contact.setUpdatedDate(new Date());
                contact.setDeleteStatus(false);
                contacts.add(contact);
            }
        }
        return contacts;
    }

    private CompanyResponseDto mapToResponseDto(Company company) {
        CompanyResponseDto dto = new CompanyResponseDto();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setCompanyGstType(company.getCompanyGstType());
        dto.setGstBusinessType(company.getGstBusinessType());
        dto.setGstNo(company.getGstNo());
        dto.setEstablishDate(company.getEstablishDate());
        dto.setIndustry(company.getIndustry());
        dto.setAddress(company.getAddress());
        dto.setCity(company.getCity());
        dto.setState(company.getState());
        dto.setCountry(company.getCountry());
        dto.setPrimaryPinCode(company.getPrimaryPinCode());
        dto.setContactIds(company.getContacts().stream()
                .filter(contact -> !contact.isDeleteStatus())
                .map(Contact::getId)
                .collect(Collectors.toList()));
        dto.setIndustries(company.getIndustries());
        dto.setSubIndustry(company.getSubIndustry());
        dto.setSubSubIndustry(company.getSubSubIndustry());
        dto.setCreatedDate(company.getCreatedDate());
        dto.setUpdatedDate(company.getUpdatedDate());
        return dto;
    }
}