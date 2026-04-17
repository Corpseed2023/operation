package com.doc.repository;

import com.doc.entity.document.LegalRequestDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegalRequestDocumentRepository extends JpaRepository<LegalRequestDocument, Long> {
    List<LegalRequestDocument> findByLegalRequestId(Long id);
}