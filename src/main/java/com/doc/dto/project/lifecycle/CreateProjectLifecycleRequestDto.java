package com.doc.dto.project.lifecycle;

import com.doc.entity.project.ProjectLifecycleAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectLifecycleRequestDto {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotNull(message = "Action type is required")
    private ProjectLifecycleAction actionType;

    @NotNull(message = "Requested by user ID is required")
    private Long requestedById;

    @NotBlank(message = "Request reason is required")
    @Size(
            min = 5,
            max = 2000,
            message = "Request reason must contain between 5 and 2000 characters"
    )
    private String requestReason;
}