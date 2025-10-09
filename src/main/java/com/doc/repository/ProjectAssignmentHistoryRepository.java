package com.doc.repository;

import com.doc.entity.project.ProjectAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectAssignmentHistoryRepository extends JpaRepository<ProjectAssignmentHistory, Long> {

    @Query("SELECT h FROM ProjectAssignmentHistory h WHERE h.project.company.id = :companyId " +
            "AND h.milestoneAssignment.milestone.id = :milestoneId " +
            "AND EXISTS (SELECT d FROM h.milestoneAssignment.productMilestoneMap.milestone.departments d WHERE d.id = :departmentId) " +
            "AND h.isDeleted = false")
    List<ProjectAssignmentHistory> findByProjectCompanyIdAndMilestoneIdAndDepartmentId(Long companyId, Long milestoneId, Long departmentId);

    @Query("SELECT h FROM ProjectAssignmentHistory h WHERE h.milestoneAssignment.milestone.id = :milestoneId " +
            "AND EXISTS (SELECT d FROM h.milestoneAssignment.productMilestoneMap.milestone.departments d WHERE d.id = :departmentId) " +
            "AND (h.project.contact.emails = :email OR h.project.contact.contactNo = :phone OR h.project.contact.whatsappNo = :phone) " +
            "AND h.isDeleted = false")
    List<ProjectAssignmentHistory> findByContactDetails(String email, String phone, Long milestoneId, Long departmentId);
}