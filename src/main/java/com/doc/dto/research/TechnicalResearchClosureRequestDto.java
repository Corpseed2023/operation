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
public class TechnicalResearchClosureRequestDto {

    /**
     * Manager, salesperson, or authorized user
     * performing the action.
     */
    @NotNull(message = "Actor user ID is required")
    @Positive(message = "Actor user ID must be greater than zero")
    private Long actorUserId;

    /**
     * Revision, rejection, or cancellation reason.
     */
    @NotBlank(message = "Reason is required")
    @Size(
            max = 2000,
            message = "Reason cannot exceed 2000 characters"
    )
    private String reason;
}