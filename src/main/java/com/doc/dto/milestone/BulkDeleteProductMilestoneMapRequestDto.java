package com.doc.dto.milestone;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BulkDeleteProductMilestoneMapRequestDto {

    @NotEmpty(message = "At least one mapping ID is required")
    private List<Long> ids;
}