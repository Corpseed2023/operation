package com.doc.entity.product;

import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.user.User;
import com.doc.entity.user.UserProductMap;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_name", columnList = "productName", unique = true)
})
@Getter @Setter @NoArgsConstructor
public class Product {

    @Id
    private Long id;

    @Column(name = "productName", nullable = false, unique = true, length = 255)
    private String productName;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    private LocalDate date;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    private Date updatedDate;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(nullable = false)
    private boolean isActive = true;

    /**
     * Does this product require client portal login at any stage?
     * Examples:
     * YES → EPR Plastic, CTO/CTE, FSSAI, BIS CRS, Pollution Board portals
     * NO  → ISO Certification, Trademark, Company Audits, Training
     */
    @Column(nullable = false)
    private boolean requiresClientPortal = false;

    /**
     * Suggested/Expected portal name shown to user as hint
     * e.g., "CPCB EPR Plastic Portal", "FoSCoS (FSSAI)", "BIS CRS Portal", "Parivesh / State SPCB"
     */
    @Column(length = 255)
    private String expectedPortalName;

    /**
     * Default portal URL (optional) – auto-filled in frontend
     * e.g., https://eprplastic.cpcb.gov.in
     */
    @Column(length = 512)
    private String defaultPortalUrl;

    // =====================================================================

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<UserProductMap> userProductMaps = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProductMilestoneMap> milestoneSteps = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductDocumentMapping> documentMappings = new ArrayList<>();
}