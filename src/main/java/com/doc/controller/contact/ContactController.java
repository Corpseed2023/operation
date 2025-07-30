package com.doc.controller.contact;



import com.doc.dto.contact.ContactRequestDto;
import com.doc.dto.contact.ContactResponseDto;
import com.doc.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@Validated
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    public ResponseEntity<ContactResponseDto> createContact(@Valid @RequestBody ContactRequestDto requestDto) {
        ContactResponseDto response = contactService.createContact(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactResponseDto> getContactById(@PathVariable Long id) {
        ContactResponseDto response = contactService.getContactById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<ContactResponseDto>> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<ContactResponseDto> responses = contactService.getAllContacts(page, size);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactResponseDto> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody ContactRequestDto requestDto) {
        ContactResponseDto response = contactService.updateContact(id, requestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
