package com.doc.repository.vendor;


import com.doc.entity.vendor.ProcurementOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<ProcurementOrder, Long> {
    Optional<ProcurementOrder> findByProcurementAssignmentId(Long procurementAssignmentId);
}