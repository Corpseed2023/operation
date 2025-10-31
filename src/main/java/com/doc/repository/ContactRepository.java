package com.doc.repository;

import com.doc.entity.client.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    boolean existsByEmailsAndCompanyIdAndDeleteStatusFalseAndIsActiveTrue(String emails, Long companyId);

    Page<Contact> findByDeleteStatusFalseAndIsActiveTrue(Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE c.id = :id AND c.deleteStatus = false AND c.isActive = true")
    Optional<Contact> findByIdAndDeleteStatusFalseAndIsActiveTrue(@Param("id") Long id);

    Page<Contact> findByCompanyIdAndDeleteStatusFalseAndIsActiveTrue(Long companyId, Pageable pageable);

    Page<Contact> findByCompanyIdAndCreatedByAndDeleteStatusFalseAndIsActiveTrue(Long companyId, Long createdBy, Pageable pageable);

    Page<Contact> findByCreatedByAndDeleteStatusFalseAndIsActiveTrue(Long createdBy, Pageable pageable);

}