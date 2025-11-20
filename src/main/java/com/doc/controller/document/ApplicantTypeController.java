package com.doc.controller.document;

// package com.doc.controller.document;

import com.doc.entity.document.ApplicantType;
import com.doc.service.ApplicantTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applicant-types")
@Validated
public class ApplicantTypeController {

    @Autowired
    private ApplicantTypeService applicantTypeService;

    // CREATE
    @PostMapping
    public ResponseEntity<ApplicantType> createApplicantType(@Valid @RequestBody ApplicantType applicantType) {
        ApplicantType created = applicantTypeService.createApplicantType(applicantType);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // GET ALL ACTIVE
    @GetMapping
    public ResponseEntity<List<ApplicantType>> getAllActiveApplicantTypes() {
        List<ApplicantType> types = applicantTypeService.getAllActiveApplicantTypes();
        return ResponseEntity.ok(types);
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<ApplicantType> getApplicantTypeById(@PathVariable Long id) {
        ApplicantType type = applicantTypeService.getApplicantTypeById(id);
        return ResponseEntity.ok(type);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<ApplicantType> updateApplicantType(
            @PathVariable Long id,
            @Valid @RequestBody ApplicantType applicantType) {
        ApplicantType updated = applicantTypeService.updateApplicantType(id, applicantType);
        return ResponseEntity.ok(updated);
    }

    // SOFT DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplicantType(@PathVariable Long id) {
        applicantTypeService.softDeleteApplicantType(id);
        return ResponseEntity.noContent().build();
    }
}