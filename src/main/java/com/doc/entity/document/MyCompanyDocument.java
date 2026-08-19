package com.doc.entity.document;

import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Entity
@Table(
        name = "my_company_documents",
        indexes = {
                @Index(name = "idx_mcd_document_type", columnList = "document_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@Comment("Documents for the (single, fixed) company — e.g. PAN, Aadhar, Cancelled Cheque")
public class MyCompanyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_type", nullable = false, length = 100)
    @Comment("Free-text category, e.g. 'PAN Card', 'Aadhar Card', 'GST Certificate'")
    private String documentType;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size_kb")
    private Integer fileSizeKb;

    @Column(name = "file_format", length = 10)
    private String fileFormat;

    @Column(name = "document_number", length = 100)
    @Comment("PAN number, GST number, bank account number etc. as applicable")
    private String documentNumber;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "upload_time", nullable = false)
    private Date uploadTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", nullable = false, updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date", nullable = false)
    private Date updatedDate;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        this.createdDate = now;
        this.updatedDate = now;
        if (this.uploadTime == null) {
            this.uploadTime = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedDate = new Date();
    }
}