package com.doc.entity.milestone;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "milestone_statuses", indexes = {
        @Index(name = "idx_name", columnList = "name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class MilestoneStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Milestone status ID")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("Status name (e.g., NEW, IN_PROGRESS)")
    private String name;

    @Column(columnDefinition = "varchar(1000)")
    @Comment("Detailed description of the status")
    private String description;
}