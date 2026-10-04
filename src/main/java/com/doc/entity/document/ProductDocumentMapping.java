package com.doc.entity.document;

import com.doc.entity.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(
        name = "product_document_mapping",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_document_applicant",
                        columnNames = {
                                "product_id",
                                "required_document_id",
                                "applicant_type_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_pdm_product",
                        columnList = "product_id"
                ),
                @Index(
                        name = "idx_pdm_applicant_type",
                        columnList = "applicant_type_id"
                ),
                @Index(
                        name = "idx_pdm_document",
                        columnList = "required_document_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "required_document_id",
            nullable = false
    )
    private ProductRequiredDocuments requiredDocument;

    /*
     * OPTIONAL.
     *
     * null = global document for the product.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "applicant_type_id",
            nullable = true
    )
    private ApplicantType applicantType;

    @Column(
            name = "is_mandatory",
            nullable = false
    )
    private boolean isMandatory = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(
            name = "is_active",
            nullable = false
    )
    private boolean isActive = true;

    @Column(
            name = "created_by",
            nullable = false
    )
    private Long createdBy;

    @Column(
            name = "updated_by",
            nullable = false
    )
    private Long updatedBy;

    @Column(
            name = "created_date",
            updatable = false
    )
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Column(name = "updated_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    @PrePersist
    protected void onCreate() {

        Date now = new Date();

        this.createdDate = now;
        this.updatedDate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = new Date();
    }
}