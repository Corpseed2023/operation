package com.doc.entity.project;

import com.doc.entity.product.ProductRequiredDocuments;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Represents the document uploaded for a specific project against a required document item.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProjectDocumentUpload {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pduid")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pid", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rdid", nullable = false)
    private ProductRequiredDocuments requiredDocument;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "status")
    private String status; // e.g., PENDING, UPLOADED, VERIFIED, REJECTED

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "upload_time")
    private Date uploadTime;

    @Column(name = "isd", nullable = false)
    private boolean isDeleted = false;
}
