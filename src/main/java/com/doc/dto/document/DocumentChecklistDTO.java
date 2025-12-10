// File: src/main/java/com/doc/dto/document/DocumentChecklistDTO.java
package com.doc.dto.document;

import lombok.Data;

import java.util.Date;

@Data
public class DocumentChecklistDTO {

    // === From ProductDocumentMapping (required document definition) ===
    private Long documentId;                    // ProductRequiredDocuments.id
    private String documentName;                // e.g., "Aadhar Card"
    private String documentType;                // IDENTITY, FINANCIAL, etc.
    private boolean isMandatory;                // from ProductDocumentMapping
    private Integer displayOrder;               // for sorting

    // === From ProjectDocumentUpload (actual uploaded file) ===
    private Long uploadId;                      // ProjectDocumentUpload.id (null if not uploaded)
    private String fileUrl;                     // S3 link
    private String fileName;                    // Original filename
    private Date uploadedAt;                    // upload time
    private String uploadedByName;              // Full name of uploader (optional)
    private Long uploadedById;                  // User ID

    // === Status & Verification ===
    private String status;                      // "PENDING", "UPLOADED", "VERIFIED", "REJECTED"
    private boolean verified;                   // true if status == VERIFIED
    private String remarks;                     // Legal person remarks (on reject)
    private int replacementCount;               // How many times replaced

    // === Extra info (optional but useful) ===
    private Date expiryDate;                    // if document has expiry
    private boolean isExpired;                  // auto-calculated
    private boolean isPermanent;                // no expiry
    private String allowedFormats;              // "pdf,jpg,png"
    private Integer minFileSizeKb;              // validation hint
}