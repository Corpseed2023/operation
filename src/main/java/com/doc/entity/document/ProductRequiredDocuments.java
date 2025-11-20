// src/main/java/com/doc/entity/document/ProductRequiredDocuments.java

package com.doc.entity.document;

import com.doc.em.DocumentExpiryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "product_required_documents",
        indexes = {@Index(name = "idx_name", columnList = "name")},
        uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "country", "centralName", "stateName"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequiredDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary Key: Unique identifier")
    private Long id;

    @Column(nullable = false, length = 255)
    @Comment("Document name (e.g., Aadhaar Card, PAN Card)")
    private String name;

    @Column(length = 1000)
    @Comment("Detailed description")
    private String description;

    @Column(nullable = false, length = 50)
    @Comment("Type: IDENTITY, FINANCIAL, PROOF, ADDRESS, etc.")
    private String type;

    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT ''")
    @Comment("Country (empty for central)")
    private String country = "";

    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT ''")
    @Comment("Central govt name")
    private String centralName = "";

    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT ''")
    @Comment("State name")
    private String stateName = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("FIXED = never expires, EXPIRING = has expiry, UNKNOWN = CRT decides")
    private DocumentExpiryType expiryType = DocumentExpiryType.UNKNOWN;

    @Column(name = "is_mandatory", nullable = false)
    @Comment("Default mandatory flag (can be overridden per product/applicant)")
    private boolean isMandatory = true;

    @Column(name = "max_validity_years")
    @Comment("Max years for EXPIRING docs (e.g., 3 for CTO)")
    private Integer maxValidityYears;

    @Column(name = "min_file_size_kb")
    @Comment("Min file size in KB")
    private Integer minFileSizeKb;

    @Column(name = "allowed_formats", length = 100)
    @Comment("Comma-separated: pdf,jpg,png")
    private String allowedFormats = "pdf,jpg,png";

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", nullable = false, updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date", nullable = false)
    private Date updatedDate;

    // REMOVED OLD ManyToMany
    // @ManyToMany → product_document_mapping (join table)

    // NEW: One-to-many to the new mapping table
    @OneToMany(mappedBy = "requiredDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductDocumentMapping> productMappings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = new Date();
    }

    public boolean isFixed() {
        return expiryType == DocumentExpiryType.FIXED;
    }

    public boolean isExpiring() {
        return expiryType == DocumentExpiryType.EXPIRING;
    }
}