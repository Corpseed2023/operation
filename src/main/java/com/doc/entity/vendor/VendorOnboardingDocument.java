package com.doc.entity.vendor;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(
        name = "vendor_onboarding_documents",
        indexes = {
                @Index(name = "idx_vendor_onboarding_doc_onboarding", columnList = "vendor_onboarding_id"),
                @Index(name = "idx_vendor_onboarding_doc_type", columnList = "document_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorOnboardingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_onboarding_id", nullable = false)
    private VendorOnboarding vendorOnboarding;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 100)
    private VendorOnboardingDocumentType documentType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(length = 1000)
    private String remarks;

    private Long uploadedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date uploadedDate;

    private boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        this.uploadedDate = new Date();
    }
}