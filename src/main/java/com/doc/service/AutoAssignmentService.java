package com.doc.service;

import com.doc.dto.DepartmentAutoConfigDto;
import com.doc.entity.product.ProductMilestoneMap;
import com.doc.entity.project.AssignmentResult;

public interface AutoAssignmentService {
    AssignmentResult assignMilestoneUser(ProductMilestoneMap milestone, Long updatedById);
    void updateDepartmentAutoConfig(DepartmentAutoConfigDto dto);

    DepartmentAutoConfigDto getDepartmentAutoConfig(Long id);
}