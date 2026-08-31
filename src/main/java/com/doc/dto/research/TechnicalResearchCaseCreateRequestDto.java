package com.doc.dto.research;

import com.doc.entity.research.ResearchPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalResearchCaseCreateRequestDto {

    /**
     * Optional Lead Service reference.
     */
    @Positive(message = "Originating lead ID must be greater than zero")
    private Long originatingLeadId;

    /**
     * Optional Solution reference from Lead Service.
     */
    @Positive(message = "Originating solution ID must be greater than zero")
    private Long originatingSolutionId;

    /**
     * Snapshot of the solution name.
     *
     * Example: FSSAI Central License
     */
    @Size(
            max = 255,
            message = "Solution name cannot exceed 255 characters"
    )
    private String solutionName;

    /**
     * Operation Service product.
     */
    @NotNull(message = "Product ID is required")
    @Positive(message = "Product ID must be greater than zero")
    private Long productId;

    /**
     * Research subject.
     *
     * Example: FSSAI Central Licence Eligibility Assessment
     */
    @NotBlank(message = "Research subject is required")
    @Size(
            max = 500,
            message = "Research subject cannot exceed 500 characters"
    )
    private String subject;

    /**
     * Customer requirement and business background.
     */
    @Size(
            max = 20000,
            message = "Business context cannot exceed 20000 characters"
    )
    private String businessContext;

    /**
     * Questions and areas that must be researched.
     */
    @Size(
            max = 20000,
            message = "Research scope cannot exceed 20000 characters"
    )
    private String researchScope;


    /**
     * Salesperson raising the research case.
     */
    @NotNull(message = "Raised-by user ID is required")
    @Positive(message = "Raised-by user ID must be greater than zero")
    private Long raisedByUserId;

    /**
     * Defaults to MEDIUM when not provided.
     */
    private ResearchPriority priority;

    @FutureOrPresent(
            message = "Due date must be today or a future date"
    )
    private LocalDate dueDate;
}