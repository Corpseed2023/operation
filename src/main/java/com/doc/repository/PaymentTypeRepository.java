package com.doc.repository;


import com.doc.entity.client.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTypeRepository extends JpaRepository<PaymentType, Long> {

    boolean existsByNameAndIsDeletedFalse(String name);

    Page<PaymentType> findByIsDeletedFalse(Pageable pageable);
}
