package com.doc.dto.research;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalResearchSubmissionRequestDto {

    /**
     * Currently assigned technical user.
     */
    @NotNull(message = "Submitted-by user ID is required")
    @Positive(message = "Submitted-by user ID must be greater than zero")
    private Long submittedByUserId;

    /**
     * Mandatory research findings.
     */
    @NotBlank(message = "Research findings are required")
    @Size(
            max = 50000,
            message = "Research findings cannot exceed 50000 characters"
    )
    private String findings;

    /**
     * Recommended licence type, process, documents,
     * government fee, timeline, or other conclusion.
     */
    @Size(
            max = 30000,
            message = "Recommendation cannot exceed 30000 characters"
    )
    private String recommendation;
}