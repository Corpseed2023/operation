//package com.doc.entity.document;
//
//import com.doc.entity.product.Product;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//
//@Entity
//@Table(name = "product_applicant_type",
//        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "applicant_type_id"}))
//@Getter @Setter
//public class ProductApplicantType {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "product_id", nullable = false)
//    private Product product;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "applicant_type_id", nullable = false)
//    private ApplicantType applicantType;
//
//    private Integer displayOrder;
//
//    private boolean isMandatory = false; // e.g., Brand Owner mandatory for EPR
//
//    @Column(nullable = false)
//    private boolean isActive = true;
//}