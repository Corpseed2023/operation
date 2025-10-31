package com.doc.repository.documentRepo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.doc.entity.document.DocumentStatus;


import java.util.Optional;

@Repository
public interface DocumentStatusRepository extends JpaRepository<DocumentStatus, Long> {
    Optional<DocumentStatus> findByName(String name);
}