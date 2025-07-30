package com.doc.repsoitory;


import com.doc.entity.client.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    boolean existsByEmailsAndCompanyIdAndDeleteStatusFalse(String emails, Long companyId);

    Page<Contact> findByDeleteStatusFalse(Pageable pageable);

    Page<Contact> findByCompanyIdAndDeleteStatusFalse(Long companyId, Pageable pageable);
}
