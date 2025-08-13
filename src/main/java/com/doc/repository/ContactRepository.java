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

    boolean existsByEmailsAndCompanyIdAndDeleteStatusFalse(String emails, Long companyId);

    Page<Contact> findByDeleteStatusFalse(Pageable pageable);

    Page<Contact> findByCompanyIdAndDeleteStatusFalse(Long companyId, Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE c.id = :id AND c.deleteStatus = false")
    Optional<Contact> findByIdAndDeleteStatusFalse(@Param("id") Long id);
}