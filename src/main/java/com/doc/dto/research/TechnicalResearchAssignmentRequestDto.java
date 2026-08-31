package com.doc.dto.research;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class TechnicalResearchAssignmentRequestDto {

    /**
     * Manager performing the assignment.
     */
    @NotNull(message = "Assigned-by user ID is required")
    @Positive(message = "Assigned-by user ID must be greater than zero")
    private Long assignedByUserId;

    /**
     * Technical person who will work on the research.
     */
    @NotNull(message = "Assignee user ID is required")
    @Positive(message = "Assignee user ID must be greater than zero")
    private Long assigneeUserId;

    /**
     * Manager can define or revise the due date.
     */
    @FutureOrPresent(
            message = "Due date must be today or a future date"
    )
    private LocalDate dueDate;
}