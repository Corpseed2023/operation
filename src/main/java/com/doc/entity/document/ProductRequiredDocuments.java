package com.doc.entity.document;

import com.doc.em.DocumentExpiryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "product_required_documents",
        indexes = {
                @Index(name = "idx_name", columnList = "name")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "country", "centralName", "stateName"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequiredDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, length = 50)
    private String type; // IDENTITY, ADDRESS, FINANCIAL, etc.

    @Column(columnDefinition = "VARCHAR(255) DEFAULT ''")
    private String country = "";

    @Column(columnDefinition = "VARCHAR(255) DEFAULT ''")
    private String centralName = "";

    @Column(columnDefinition = "VARCHAR(255) DEFAULT ''")
    private String stateName = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentExpiryType expiryType = DocumentExpiryType.UNKNOWN;

    @Column(length = 1000)
    private String expiryTypeDescription;

    @Column(name = "is_mandatory")
    private boolean mandatory;

    @Column(name = "max_validity_years")
    private String maxValidityYears;

    @Column(name = "max_file_size_kb")
    @Comment("Maximum allowed file size in KB. If null = no limit")
    private Integer maxFileSizeKb;

    @Column(name = "allowed_formats", length = 100)
    private String allowedFormats = "pdf,jpg,png";

    @Column(name = "applicability", length = 500)
    @Comment("Defines where this document is applicable (e.g., Individual, Company, Both, Specific Product, etc.)")
    private String applicability;

    @Column(name = "remarks", length = 1000)
    @Comment("Additional remarks or notes about this required document")
    private String remarks;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    private Date updatedDate;

    // Reverse mapping
    @OneToMany(mappedBy = "requiredDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductDocumentMapping> productMappings = new ArrayList<>();
}