package com.doc.dto.project;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MilestoneApplicantTypeUpdateDto {
    @NotNull
    private Long applicantTypeId;

    @NotNull
    private Long updatedById;


}