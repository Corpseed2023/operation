package com.doc.dto.research;

import com.doc.entity.research.TechnicalResearchCaseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TechnicalResearchCaseStatusUpdateRequestDto {

    @NotNull(message = "Status is required")
    private TechnicalResearchCaseStatus status;

    @NotNull(message = "Updated-by user ID is required")
    @Positive(message = "Updated-by user ID must be greater than zero")
    private Long updatedByUserId;

    @Size(
            max = 2000,
            message = "Reason cannot exceed 2000 characters"
    )
    private String reason;


}