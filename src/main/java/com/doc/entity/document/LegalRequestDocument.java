package com.doc.entity.document;

import com.doc.entity.legalrequest.LegalRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "legal_request_documents")
@Getter
@Setter
@NoArgsConstructor
public class LegalRequestDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileUrl;
    private String fileType;
    private long fileSize;
    private String uuid;
    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "legal_request_id")
    private LegalRequest legalRequest;

}
