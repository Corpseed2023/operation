package com.doc.repository;
import com.doc.entity.document.MyCompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MyCompanyDocumentRepository extends JpaRepository<MyCompanyDocument, Long> {

    List<MyCompanyDocument> findAll();

    Optional<MyCompanyDocument> findByRequiredDocument_Id(Long requiredDocumentId);
}