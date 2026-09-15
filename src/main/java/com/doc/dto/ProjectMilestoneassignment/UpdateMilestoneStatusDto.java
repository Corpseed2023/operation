package com.doc.dto.ProjectMilestoneassignment;

import com.doc.em.CertificateValidityType;
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
     * Optional acknowledgement/supporting document.
     */
    private String acknowledgementAttachmentUrl;

    /*
     * Optional original/display filename.
     */
    private String acknowledgementAttachmentName;

    /*
     * FIXED_TERM or LIFETIME.
     *
     * Required only when Certification milestone
     * is being completed.
     */
    private CertificateValidityType certificateValidityType;

    /*
     * Certificate issue/effective date.
     *
     * Required for both FIXED_TERM and LIFETIME.
     */
    private LocalDate certificateIssueDate;

    /*
     * Required only for FIXED_TERM.
     *
     * Example:
     * 5 YEARS
     */
    private Integer certificationTenure;

    /*
     * Required only for FIXED_TERM.
     */
    private CertificationTenureUnit certificationTenureUnit;

    /*
     * Required only for FIXED_TERM.
     *
     * Must be null for LIFETIME.
     */
    private LocalDate certificateExpiryDate;

    /*
     * Required while completing Certification milestone.
     */
    private String certificationAttachmentUrl;


}