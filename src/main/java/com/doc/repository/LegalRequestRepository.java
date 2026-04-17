package com.doc.repository;

import com.doc.entity.LegalRequest.LegalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LegalRequestRepository extends JpaRepository<LegalRequest, Long>, JpaSpecificationExecutor<LegalRequest> {

    List<LegalRequest> findAll();

    Page<LegalRequest> findByAssignedTo(Long assignedTo, Pageable pageable);


}