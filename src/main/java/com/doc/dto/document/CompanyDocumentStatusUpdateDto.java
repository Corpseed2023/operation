package com.doc.dto.document;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyDocumentStatusUpdateDto {

    @NotBlank
    private String newStatus; // "VERIFIED", "REJECTED"

    private String remarks;   // Required if REJECTED

    @NotNull
    private Long changedById;
}