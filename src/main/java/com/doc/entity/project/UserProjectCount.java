package com.doc.entity.project;

import com.doc.entity.product.Product;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

/**
 * Entity representing the count of projects assigned to a user per product.
 */
@Entity
@Table(name = "user_project_count")
@Getter
@Setter
@NoArgsConstructor
public class UserProjectCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Unique identifier for the user project count record")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("User associated with the project count")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @Comment("Product associated with the project count")
    private Product product;

    @Column(name = "project_count", nullable = false)
    @Comment("Number of projects assigned to the user for this product")
    private int projectCount = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_updated_date", nullable = false)
    @Comment("Last date when the project count was updated")
    private Date lastUpdatedDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Created date")
    private Date createdDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Updated date")
    private Date updatedDate = new Date();

    @Comment("Created by user ID")
    private Long createdBy;

    @Comment("Updated by user ID")
    private Long updatedBy;

    @Comment("Is deleted flag (soft delete)")
    private boolean isDeleted = false;
}