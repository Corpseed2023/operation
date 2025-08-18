package com.doc.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: User ID")
    private Long id;

    @Column(name = "full_name")
    @Comment("Users full name") // Removed single quote
    private String fullName;

    @Column(name = "email")
    @Comment("Users email address") // Removed single quote
    private String email;

    @Column(name = "contact_no")
    @Comment("Users contact number") // Removed single quote
    private String contactNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    @Comment("Designation associated with the user")
    private Designation userDesignation;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_department_map",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "dept_id", referencedColumnName = "id")
    )
    @Comment("List of departments associated with the user")
    private List<Department> departments = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    @Comment("List of roles assigned to the user")
    private List<Role> roles = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    @Comment("Users manager") // Removed single quote
    private User manager;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Comment("List of product mappings for the user")
    private List<UserProductMap> userProductMaps = new ArrayList<>();

    @Column(name = "is_manager", nullable = false)
    @Comment("Flag indicating if the user is a manager")
    private boolean managerFlag = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date")
    @Comment("Creation date")
    private Date createdDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    @Comment("Update date")
    private Date updatedDate;

    private LocalDate date;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;
}