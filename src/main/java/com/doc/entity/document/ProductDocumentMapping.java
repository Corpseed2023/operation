package com.doc.entity.document;

import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.product.Product;
import com.doc.entity.user.User;
import com.doc.entity.user.UserProductMap;
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
@Table(name = "product_document_mapping",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"product_id", "required_document_id", "applicant_type_id"}
        ))
@Getter @Setter
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

    // NULL = applies to all applicant types (global)
    // Not null = only for this applicant type
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_type_id", nullable = true)
    private ApplicantType applicantType;

    private boolean isMandatory = true;
    private Integer displayOrder;
    private boolean isActive = true;
}
