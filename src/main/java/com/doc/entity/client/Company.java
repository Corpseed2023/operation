package com.doc.entity.client;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
@Entity
@Table(name = "company")
@Getter
@Setter
public class Company {

    @Id
    @Comment("Primary key: Company ID (shared / synced from account-service)")
    private Long id;

    @Column(nullable = false, length = 255)
    @Comment("Company / Group / Brand Name (e.g. Happy Bites Foods Pvt Ltd)")
    private String name;

    @Column(name = "com_pan", length = 15, unique = true)
    @Comment("Company PAN (group level)")
    private String panNo;

    @Temporal(TemporalType.DATE)
    @Comment("Company establishment / incorporation date")
    private Date establishDate;

    @Column(length = 100)
    @Comment("Top-level industry category")
    private String industry;

    @Column(length = 100)
    @Comment("Detailed / mapped industry code or name")
    private String industries;

    @Column(length = 100)
    @Comment("Sub-industry")
    private String subIndustry;

    @Column(length = 100)
    @Comment("Sub-sub-industry / detailed classification")
    private String subSubIndustry;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    @Comment("Creation timestamp")
    private Date createdDate = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    @Comment("Last update timestamp")
    private Date updatedDate = new Date();

    @Comment("Created by user ID")
    private Long createdBy;

    @Comment("Updated by user ID")
    private Long updatedBy;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("All units / branches / outlets belonging to this company")
    private List<CompanyUnit> units = new ArrayList<>();

    // Helpers
    public void addUnit(CompanyUnit unit) {
        units.add(unit);
        unit.setCompany(this);
    }

    public CompanyUnit getPrimaryUnit() {
        return units.stream()
                .filter(u -> !u.isDeleted() && "Active".equals(u.getStatus()))
                .findFirst()
                .orElse(null);
    }
}