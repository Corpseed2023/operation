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
    public ContactResponseDto createContact(ContactRequestDto requestDto) {
        return null;
    }

    @Override
    public ContactResponseDto getContactById(Long id) {
        return null;
    }
}