package com.doc.repository.documentRepo;

import com.doc.entity.document.ProductRequiredDocuments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface ProductRequiredDocumentsRepository extends JpaRepository<ProductRequiredDocuments, Long> {

    Page<ProductRequiredDocuments> findByIsDeletedFalse(Pageable pageable);

    List<ProductRequiredDocuments> findAllByIdInAndIsActiveTrueAndIsDeletedFalse(List<Long> requiredDocumentIds);


}