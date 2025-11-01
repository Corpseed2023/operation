package com.doc.entity.document;

import com.doc.em.DocumentExpiryType;
import com.doc.entity.product.Product;
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
@Comment("Master list of required documents with MNC compliance rules")
public class ProductRequiredDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary Key: Unique identifier")
    private Long id;

    @Column(nullable = false, length = 255)
    @Comment("Document name (e.g., Aadhaar Card)")
    private String name;

    @Column(length = 1000)
    @Comment("Detailed description")
    private String description;

    @Column(nullable = false, length = 50)
    @Comment("Type: IDENTITY, FINANCIAL, PROOF, etc.")
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

    @Column(name = "standard_level", length = 20, nullable = false)
    @Comment("MNC, SME, STARTUP, GOVERNMENT – affects validation strictness")
    private String standardLevel = "MNC";

    @Column(name = "is_mandatory", nullable = false)
    @Comment("Is this document mandatory?")
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

    @Column(name = "validation_regex", length = 500)
    @Comment("Regex for filename/content (e.g., PAN: ^[A-Z]{5}[0-9]{4}[A-Z]{1}$)")
    private String validationRegex;

    @Column(name = "is_gst_specific", nullable = false)
    @Comment("GST-specific document?")
    private boolean isGstSpecific = false;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_document_mapping",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Comment("Products that require this document")
    private List<Product> products = new ArrayList<>();

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