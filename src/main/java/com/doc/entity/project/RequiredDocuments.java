package com.doc.entity.project;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

/**
 * Represents a required document for a product based on region (state/central/international).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequiredDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rdid")
    @Comment("Primary Key: Unique identifier for the required document")
    private Long id;

    @Column(name = "nm", nullable = false)
    @Comment("Document Name")
    private String name;

    @Column(name = "`desc`", length = 1000)
    @Comment("Document Description")
    private String description;

    @Column(name = "tp", nullable = false)
    @Comment("Document Type")
    private String type;

    @Column(name = "ctry")
    @Comment("Country")
    private String country; // For International

    @Column(name = "cname")
    @Comment("Central Government Name")
    private String centralName; // For Central Govt level

    @Column(name = "sname")
    @Comment("State Name")
    private String stateName; // For State level

    @Column(name = "isd", nullable = false)
    @Comment("Is Deleted flag (soft delete)")
    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdt", updatable = false)
    @Comment("Created Date")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "udt")
    @Comment("Updated Date")
    private Date updatedDate;
}