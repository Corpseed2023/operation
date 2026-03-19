package com.doc.entity.project;

import com.doc.entity.client.Company;
import com.doc.entity.client.CompanyUnit;
import com.doc.entity.client.Contact;
import com.doc.entity.document.ApplicantType;
import com.doc.entity.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "project", indexes = {
        @Index(name = "idx_project_no", columnList = "projectNo", unique = true),
        @Index(name = "idx_product_id", columnList = "product_id"),
        @Index(name = "idx_sales_person_id", columnList = "sales_person_id"),
        @Index(name = "idx_status_id", columnList = "status_id"),
        @Index(name = "idx_unbilled_no", columnList = "unbilled_number"),
        @Index(name = "idx_estimate_no", columnList = "estimate_number"),
        @Index(name = "idx_company_id", columnList = "company_id"),
        @Index(name = "idx_unit_id", columnList = "unit_id")   // added for performance on unit-level queries
})
@Getter
@Setter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique identifier for the project")
    private Long id;

    @Column(nullable = false)
    @Comment("Project name (e.g. FSSAI Renewal - Noida Sector 62)")
    private String name;

    @Column(nullable = false, unique = true)
    @Comment("Unique project number")
    private String projectNo;

    @Column(name = "unbilled_number", length = 50)
    @Comment("Unbilled number – optional, unique when present")
    private String unbilledNumber;

    @Column(name = "estimate_number", length = 50)
    @Comment("Estimate number – optional, unique when present")
    private String estimateNumber;

    // Sales fields from microservice
    @Column(name = "sales_person_id")
    @Comment("Sales person ID from microservice")
    private Long salesPersonId;

    @Column(name = "sales_person_name")
    @Comment("Sales person name from microservice")
    private String salesPersonName;

    @OneToOne(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Comment("Payment details for the project")
    private ProjectPaymentDetail paymentDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @Comment("Product / service associated with the project")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    @Comment("Primary contact person for this project (usually unit-level)")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @Comment("Parent company (group level)")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    @Comment("Specific unit/branch/outlet this project belongs to (critical for compliance)")
    private CompanyUnit unit;

    @Comment("Lead ID from lead service")
    private Long leadId;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Created date")
    private Date createdDate;

    @Comment("Project start / target date")
    private LocalDate date;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Updated date")
    private Date updatedDate;

    @Comment("Created by user ID")
    private Long createdBy;

    @Comment("Updated by user ID")
    private Long updatedBy;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Is deleted flag (soft delete)")
    private boolean isDeleted = false;

    @Column(name = "is_active", nullable = false)
    @Comment("Is active flag")
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    @Comment("Project overall status")
    private ProjectStatus status;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("List of milestone assignments for this project")
    private List<ProjectMilestoneAssignment> milestoneAssignments = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_type_id")
    @Comment("Applicant type (individual / company / branch etc.)")
    private ApplicantType applicantType;


}