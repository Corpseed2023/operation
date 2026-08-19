package com.doc.repository;

import com.doc.entity.document.MyCompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MyCompanyDocumentRepository extends JpaRepository<MyCompanyDocument, Long> {

    List<MyCompanyDocument> findByDocumentTypeIgnoreCase(String documentType);
}