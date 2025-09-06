package com.doc.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents a required document for a product based on region (state/central/international).
 * Ensures uniqueness of documents based on name, country, centralName, and stateName.
 */
@Entity
@Table(name = "product_required_documents",
        indexes = {@Index(name = "idx_name", columnList = "name"), @Index(name = "idx_uuid", columnList = "uuid")},
        uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "country", "centralName", "stateName"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequiredDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary Key: Unique identifier for the required document")
    private Long id;

    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "uuid", updatable = false, nullable = false, unique = true)
    @Comment("Secondary unique identifier (UUID) for the required document")
    private UUID uuid;

    @Column(nullable = false)
    @Comment("Name of the required document (e.g., Aadhaar Card, PAN Card)")
    private String name;

    @Column(length = 1000)
    @Comment("Description of the document")
    private String description;

    @Column(nullable = false)
    @Comment("Type of document (e.g., IDENTITY, FINANCIAL, PROOF)")
    private String type;

    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT ''")
    @Comment("Country for which the document is required (empty for central/international)")
    private String country = "";

    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT ''")
    @Comment("Central government name for central-level documents (empty for state/international)")
    private String centralName = "";

    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT ''")
    @Comment("State name for state-level documents (empty for central/international)")
    private String stateName = "";

    @Column(name = "created_by", nullable = false)
    @Comment("ID of the user who created the document")
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    @Comment("ID of the user who last updated the document")
    private Long updatedBy;

    @Column(nullable = false)
    @Comment("Is Deleted flag (soft delete)")
    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", nullable = false, updatable = false)
    @Comment("Date when the document was created")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date", nullable = false)
    @Comment("Date when the document was last updated")
    private Date updatedDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_document_mapping",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Comment("List of products associated with this document")
    private List<Product> products = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();
        if (this.uuid == null) { // Fallback to ensure UUID is set
            this.uuid = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = new Date();
    }
}