package com.doc.service;

import com.doc.dto.contact.ContactRequestDto;
import com.doc.dto.contact.ContactResponseDto;

import java.util.List;

public interface ContactService {

    ContactResponseDto createContact(ContactRequestDto requestDto);

    ContactResponseDto getContactById(Long id);

    List<ContactResponseDto> getAllContacts(int page, int size);

    ContactResponseDto updateContact(Long id, ContactRequestDto requestDto);

    void deleteContact(Long id);
}
