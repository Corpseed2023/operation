package com.doc.entity.vendor;

import com.doc.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(
        name = "product_vendor_mapping",
        indexes = {
                @Index(name = "idx_pvm_product_id", columnList = "product_id"),
                @Index(name = "idx_pvm_vendor_id", columnList = "vendor_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVendorMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Product like CDSO / CDSCO / FSSAI / BIS etc.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Vendor like Balaji Trader, Vishu Trader etc.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "email_subject", length = 500)
    private String emailSubject;

    @Column(name = "email_body", columnDefinition = "LONGTEXT")
    private String emailBody;

    // Store uploaded agreement URL / S3 URL here
    @Column(name = "agreement_attachment", length = 1000)
    private String agreementAttachment;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedDate;

    private Long createdBy;
    private Long updatedBy;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean isDeleted = false;
}