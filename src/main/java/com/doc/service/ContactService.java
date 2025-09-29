package com.doc.service;

import com.doc.dto.contact.ContactRequestDto;
import com.doc.dto.contact.ContactResponseDto;

import java.util.List;

public interface ContactService {


    List<ContactResponseDto> getAllContacts(int page, int size, Long companyId, Long userId);

    ContactResponseDto updateContact(Long id, ContactRequestDto requestDto);

    void deleteContact(Long id);

    ContactResponseDto createContact(ContactRequestDto requestDto);

    ContactResponseDto getContactById(Long id);
}
