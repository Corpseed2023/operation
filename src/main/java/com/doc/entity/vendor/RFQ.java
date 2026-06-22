package com.doc.entity.vendor;

import com.doc.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "rfqs", indexes = {
        @Index(name = "idx_rfq_number", columnList = "rfqNumber", unique = true),
        @Index(name = "idx_rfq_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFQ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    @Comment("Unique RFQ Number like RFQ-2026-0001")
    private String rfqNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(length = 1000)
    private String scopeOfWork;

    @Column(length = 1000)
    private String termsAndConditions;

    @Column(length = 500)
    private String deliveryLocation;

    @Temporal(TemporalType.DATE)
    private Date quotationSubmissionDeadline;

    @Temporal(TemporalType.DATE)
    private Date expectedStartDate;

    @Temporal(TemporalType.DATE)
    private Date expectedEndDate;

    @Column(length = 255)
    private String contactPersonName;

    @Column(length = 255)
    private String contactPersonEmail;

    @Column(length = 20)
    private String contactPersonMobile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RFQStatus status = RFQStatus.DRAFT;

    @Column(length = 500)
    private String attachmentUrl;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private Long createdBy;
    private Long updatedBy;

    private boolean isDeleted = false;

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RFQVendor> vendors = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        this.createdDate = new Date();
        this.updatedDate = new Date();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedDate = new Date();
    }
}