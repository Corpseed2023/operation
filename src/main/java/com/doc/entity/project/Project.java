package com.doc.entity.project;

import com.doc.entity.client.Company;
import com.doc.entity.client.Contact;
import com.doc.entity.user.User;
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

/**
 * Entity representing a project in the system.
 */
@Entity
@Table(name = "project", indexes = {
        @Index(name = "idx_project_no", columnList = "projectNo", unique = true),
        @Index(name = "idx_product_id", columnList = "product_id"),
        @Index(name = "idx_sales_person_id", columnList = "sales_person_id"),
        @Index(name = "idx_status", columnList = "status")
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
    @Comment("Project name")
    private String name;

    @Column(nullable = false, unique = true)
    @Comment("Unique project number")
    private String projectNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_person_id")
    @Comment("Sales person who brought this project")
    private User salesPerson;

    @OneToOne(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Comment("Payment details for the project")
    private ProjectPaymentDetail paymentDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @Comment("Product associated with the project")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    @Comment("Contact person for the project")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @Comment("Company associated with the project")
    private Company company;

    @Comment("Lead ID from lead service")
    private Long leadId;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Created date")
    private Date createdDate;

    @Comment("Project date")
    private LocalDate date;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Updated date")
    private Date updatedDate;

    @Comment("Created by user ID")
    private Long createdBy;

    @Comment("Updated by user ID")
    private Long updatedBy;

    @Comment("Is deleted flag (soft delete)")
    private boolean isDeleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Comment("Project status: OPEN, IN_PROGRESS, COMPLETED, CANCELLED, REFUNDED")
    private ProjectStatus status = ProjectStatus.OPEN;

    @Comment("Project address")
    private String address;

    @Comment("City")
    private String city;

    @Comment("State")
    private String state;

    @Comment("Country")
    private String country;

    @Comment("Primary postal code")
    private String primaryPinCode;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("List of milestone assignments for this project")
    private List<ProjectMilestoneAssignment> milestoneAssignments = new ArrayList<>();
}