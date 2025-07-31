package com.doc.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;
import java.util.List;

/**
 * Represents a required document for a product based on region (state/central/international).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequiredDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary Key: Unique identifier for the required document")
    private Long id;

    @Column(name = "nm", nullable = false)
    private String name;

    @Column(name = "`desc`", length = 1000)
    private String description;

    @Column(name = "tp", nullable = false)
    private String type;

    @Comment("Country")
    private String country; // For International

    @Comment("Central Government Name")
    private String centralName; // For Central Govt level

    @Comment("State Name")
    private String stateName; // For State level

    @Comment("Is Deleted flag (soft delete)")
    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Created Date")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Updated Date")
    private Date updatedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

}

