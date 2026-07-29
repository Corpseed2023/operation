package com.doc.dto.ProjectMilestoneassignment;

import com.doc.em.CertificationTenureUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMilestoneStatusDto {

    @NotNull(message = "Assignment ID cannot be null")
    private Long assignmentId;

    @NotBlank(message = "New status name cannot be blank")
    private String newStatusName;

    @NotBlank(message = "Status reason cannot be blank")
    private String statusReason;

    @NotNull(message = "Changed by user ID cannot be null")
    private Long changedById;

    /*
     * The following fields are required only when:
     *
     * milestoneName = Certification
     * newStatusName = COMPLETED
     */

    private Integer certificationTenure;

    private CertificationTenureUnit certificationTenureUnit;

    private LocalDate certificateExpiryDate;

    private Long certificateDocumentId;
}