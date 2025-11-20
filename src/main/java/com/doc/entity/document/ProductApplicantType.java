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
@Table(name = "product_applicant_type",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "applicant_type_id"}))
@Getter @Setter
public class ProductApplicantType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_type_id", nullable = false)
    private ApplicantType applicantType;

    private Integer displayOrder;

    private boolean isMandatory = false; // e.g., Brand Owner mandatory for EPR

    private boolean isActive = true;
}