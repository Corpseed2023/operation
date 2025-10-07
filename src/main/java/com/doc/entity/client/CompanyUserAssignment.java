package com.doc.entity.client;


import com.doc.entity.department.Department;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Entity
@Table(name = "company_user_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyUserAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Assignment ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @Comment("Company associated with the assignment")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @Comment("Department for which the user is assigned")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_user_id", nullable = false)
    @Comment("Primary user assigned to handle the company for this department")
    private User primaryUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alternative_user_id")
    @Comment("Alternative user assigned if primary is unavailable")
    private User alternativeUser;

    @Column(nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    @Column(nullable = false)
    @Comment("Active status flag")
    private boolean isActive = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    @Comment("Creation date")
    private Date createdDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Update date")
    private Date updatedDate = new Date();

    @Comment("Created by user ID")
    private Long createdBy;

    @Comment("Updated by user ID")
    private Long updatedBy;



}