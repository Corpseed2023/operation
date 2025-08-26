package com.doc.entity.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a required document for a product based on region (state/central/international).
 */
@Entity
@Table(name = "product_required_documents", indexes = {
        @Index(name = "idx_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequiredDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary Key: Unique identifier for the required document")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String type;

    @Comment("Country")
    private String country;

    @Comment("Central Government Name")
    private String centralName;

    @Comment("State Name")
    private String stateName;

    @Comment("Is Deleted flag (soft delete)")
    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Created Date")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Updated Date")
    private Date updatedDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_document_mapping",
            joinColumns = @JoinColumn(name = "document_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Comment("List of products associated with this document")
    private List<Product> products = new ArrayList<>();
}