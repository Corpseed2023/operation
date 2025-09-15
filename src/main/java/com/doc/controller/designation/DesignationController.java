package com.doc.controller.designation;

import com.doc.dto.desigantion.DesignationRequestDto;
import com.doc.dto.desigantion.DesignationResponseDto;
import com.doc.service.DesignationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/designations")
@Validated
public class DesignationController {

    @Autowired
    private DesignationService designationService;

    @PostMapping
    public ResponseEntity<DesignationResponseDto> createDesignation(@Valid @RequestBody DesignationRequestDto requestDto) {
        DesignationResponseDto response = designationService.createDesignation(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignationResponseDto> getDesignationById(@PathVariable Long id) {
        DesignationResponseDto response = designationService.getDesignationById(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<DesignationResponseDto>> getAllDesignations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<DesignationResponseDto> responses = designationService.getAllDesignations(page, size);
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DesignationResponseDto> updateDesignation(
            @PathVariable Long id,
            @Valid @RequestBody DesignationRequestDto requestDto) {
        DesignationResponseDto response = designationService.updateDesignation(id, requestDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesignation(@PathVariable Long id) {
        designationService.deleteDesignation(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/master")
    public ResponseEntity<DesignationResponseDto> createMasterDesignation(
            @Valid @RequestBody DesignationRequestDto requestDto) {
        DesignationResponseDto response = designationService.createMasterDesignation(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}