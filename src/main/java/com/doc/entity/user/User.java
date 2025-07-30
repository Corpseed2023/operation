package com.doc.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Long id;

    private String fullName;

    private String email;

    private String contactNo;

    private String designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designation_id")
    private Designation userDesignation;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_department_map",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "dept_id", referencedColumnName = "id")
    )
    private List<Department> departments;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<UserProductMap> userProductMaps;

    private boolean isManager = false;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private boolean isDeleted = false;
}
