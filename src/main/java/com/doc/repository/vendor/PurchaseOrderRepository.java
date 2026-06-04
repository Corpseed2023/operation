package com.doc.repository.vendor;


import com.doc.entity.vendor.ProcurementOrder;
import com.doc.entity.vendor.ProcurementOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<ProcurementOrder, Long> {
    Optional<ProcurementOrder> findByProcurementAssignmentId(Long procurementAssignmentId);

    Page<ProcurementOrder> findByIsDeletedFalse(Pageable pageable);

    Page<ProcurementOrder> findByStatusAndIsDeletedFalse(
            ProcurementOrderStatus status,
            Pageable pageable
    );

    Page<ProcurementOrder> findByProjectIdAndIsDeletedFalse(
            Long projectId,
            Pageable pageable
    );


}