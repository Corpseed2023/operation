package com.doc.repository;

import com.doc.entity.legalrequest.LegalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LegalRequestRepository extends JpaRepository<LegalRequest, Long> {





}