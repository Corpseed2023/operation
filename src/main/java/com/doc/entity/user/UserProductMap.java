package com.doc.entity.user;

import com.doc.entity.product.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

/**
 * Entity representing a mapping between a user and a product for project assignment.
 */
@Entity
@Table(name = "user_product_map")
@Getter
@Setter
@NoArgsConstructor
public class UserProductMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique identifier for the user-product mapping")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("User associated with the product")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @Comment("Product associated with the user")
    private Product product;

    @Column(name = "rating")
    @Comment("Rating for the user-product assignment (used for prioritization)")
    private Double rating;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Created date")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Updated date")
    private Date updatedDate;

    @Comment("Created by user ID")
    private Long createdBy;

    @Comment("Updated by user ID")
    private Long updatedBy;

    @Comment("Is deleted flag (soft delete)")
    private boolean isDeleted = false;
}
