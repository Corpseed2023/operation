package com.doc.entity.product;

import com.doc.entity.user.Department;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.List;

/**
 * Entity representing a reusable milestone.
 */
@Entity
@Table(name = "milestones")
@Getter
@Setter
@NoArgsConstructor
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Milestone ID")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("Milestone name, unique (e.g., Documentation, Auditing)")
    private String name;

    @ManyToMany(mappedBy = "milestones", fetch = FetchType.LAZY)
    @Comment("Departments associated with this milestone")
    private List<Department> departments;


}