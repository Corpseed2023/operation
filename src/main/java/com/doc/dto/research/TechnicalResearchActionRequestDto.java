package com.doc.dto.research;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class TechnicalResearchActionRequestDto {

    /**
     * User performing the requested action.
     */
    @NotNull(message = "Actor user ID is required")
    @Positive(message = "Actor user ID must be greater than zero")
    private Long actorUserId;
}