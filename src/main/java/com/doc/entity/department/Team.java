package com.doc.entity.department;

import com.doc.entity.product.Product;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entity representing a team within a department, optionally created to handle specific products.
 * Teams define a group of users, a team lead, and associated products for specialized milestone assignments.
 */
@Entity
@Table(name = "teams", indexes = {
        @Index(name = "idx_team_name_dept_id", columnList = "name, department_id", unique = true),
        @Index(name = "idx_department_id", columnList = "department_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique identifier for the team")
    private Long id;

    @Column(nullable = false)
    @Comment("Team name, unique within the department")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    @Comment("Department to which this team belongs")
    private Department department;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "team_product_map",
            joinColumns = @JoinColumn(name = "team_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "product_id", referencedColumnName = "id")
    )
    @Comment("List of products (services) this team handles")
    private List<Product> products = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "team_user_map",
            joinColumns = @JoinColumn(name = "team_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id")
    )
    @Comment("List of users assigned to this team")
    private List<User> members = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_lead_id")
    @Comment("User designated as the team lead")
    private User teamLead;

    @Column(name = "is_active", nullable = false)
    @Comment("Flag indicating if the team is active")
    private boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    @Column(name = "is_temporary", nullable = false)
    @Comment("Flag indicating if the team is temporary (for auto-deletion scheduling)")
    private boolean isTemporary = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_date")
    @Comment("End date for temporary teams (null for permanent teams)")
    private Date endDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    @Comment("Creation date")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    @Comment("Update date")
    private Date updatedDate;

    @Comment("Created by user ID")
    private Long createdBy;

    @Comment("Updated by user ID")
    private Long updatedBy;
}