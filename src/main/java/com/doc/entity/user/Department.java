package com.doc.entity.user;

import com.doc.entity.product.Milestone;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nm", nullable = false, unique = true)
    private String name;

    @Column(name = "isd", nullable = false)
    private boolean isDeleted = false;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "dept_milestone_map",
            joinColumns = @JoinColumn(name = "dept_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "milestone_id", referencedColumnName = "id")
    )
    private List<Milestone> milestones;

    @ManyToMany(mappedBy = "departments", fetch = FetchType.LAZY)
    private List<User> users;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Designation> designations;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdt", updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "udt")
    private Date updatedDate;


}
