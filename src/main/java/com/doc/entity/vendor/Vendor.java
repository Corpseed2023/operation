package com.doc.entity.vendor;

import com.doc.entity.milestone.Milestone;
import com.doc.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "vendors", indexes = {
        @Index(name = "idx_gst_number", columnList = "gstNumber", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    @Comment("Vendor Company Name")
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String mobile;

    @Column(length = 15, unique = true)
    private String gstNumber;

    @Column(length = 10, unique = true)
    private String panNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorStatus status = VendorStatus.ACTIVE;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private Long createdBy;
    private Long updatedBy;

    private boolean isDeleted = false;


    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorContact> contacts = new ArrayList<>();


}