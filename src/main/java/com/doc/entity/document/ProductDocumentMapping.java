package com.doc.entity.document;

import com.doc.entity.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "product_document_mapping",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"product_id", "required_document_id", "applicant_type_id"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "required_document_id", nullable = false)
    private ProductRequiredDocuments requiredDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_type_id", nullable = true)
    private ApplicantType applicantType;

    @Column(nullable = false)
    private boolean isMandatory = true;

    private Integer displayOrder;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "created_date", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Column(name = "updated_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    @PrePersist
    protected void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = new Date();
    }
}