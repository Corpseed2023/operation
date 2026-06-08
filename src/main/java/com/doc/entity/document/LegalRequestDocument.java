package com.doc.entity.document;

import com.doc.entity.legalrequest.LegalRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "legal_request_documents", indexes = {
        @Index(name = "idx_legal_doc_request_id", columnList = "legal_request_id")
})
@Getter
@Setter
@NoArgsConstructor
public class LegalRequestDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", length = 1000, nullable = false)
    private String fileUrl;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uuid", length = 100)
    private String uuid;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_request_id", nullable = false)
    private LegalRequest legalRequest;
}