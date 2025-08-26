package com.doc.entity.product;

import com.doc.entity.user.User;
import com.doc.entity.user.UserProductMap;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entity representing a product.
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_name", columnList = "productName", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Product ID")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("Product name, unique")
    private String productName;

    @Column(length = 1000)
    @Comment("Product description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @Comment("User who created the product")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    @Comment("User who updated the product")
    private User updatedBy;

    @Comment("Product date")
    private LocalDate date;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    @Comment("Creation date")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    @Comment("Update date")
    private Date updatedDate;

    @Column(nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    @Column(nullable = false)
    @Comment("Active status flag")
    private boolean isActive = true;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Comment("List of user product mappings")
    private List<UserProductMap> userProductMaps;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Comment("List of milestone steps")
    private List<ProductMilestoneMap> milestoneSteps;

    @ManyToMany(mappedBy = "products", fetch = FetchType.LAZY)
    @Comment("List of required documents")
    private List<ProductRequiredDocuments> requiredDocuments = new ArrayList<>();
}