package com.doc.entity.client;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import java.util.Date;

@Entity
@Table(name = "contact",
        indexes = {
                @Index(name = "idx_contact_name", columnList = "name"),
                @Index(name = "idx_contact_contactno", columnList = "contactNo"),  // ← change to contactNo
                @Index(name = "idx_contact_whatsapp_no", columnList = "whatsappNo"),
                @Index(name = "idx_contact_is_deleted", columnList = "is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
public class Contact {

    @Id
    @Comment("Primary key: Contact ID")
    private Long id;

    @Comment("Contact Title (Mr/Ms/Dr etc.)")
    private String title;

    @Column(nullable = false)
    @Comment("Full name of the contact person")
    private String name;

    @Column(length = 500)
    @Comment("Emails – comma separated or semicolon separated")
    private String email;

    @Column(length = 20)
    @Comment("Primary mobile number")
    private String contactNo;

    @Column(length = 20)
    @Comment("WhatsApp number")
    private String whatsappNo;

    @Comment("Designation from client side (how client calls this person)")
    private String clientDesignation;

    @Comment("Our internal understanding of designation")
    private String designation;

    @Column(nullable = false)
    @Comment("Soft delete flag – logical delete")
    private boolean deleteStatus = false;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag (standard across system)")
    private boolean isDeleted = false;

    // ──────────────────────────────────────────────
    // Company & Unit relationships – BOTH can be set
    // ──────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @Comment("Company (group/head office level) this contact belongs to")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_unit_id")
    @Comment("Specific unit/branch/outlet this contact is responsible for")
    private CompanyUnit companyUnit;

    // ──────────────────────────────────────────────
    // Primary / Secondary flags – very useful in practice
    // ──────────────────────────────────────────────

    @Column(nullable = false)
    @Comment("Is this the primary contact for the entire company?")
    private boolean isPrimaryForCompany = false;

    @Column(nullable = false)
    @Comment("Is this a secondary (backup) contact for the company?")
    private boolean isSecondaryForCompany = false;

    @Column(nullable = false)
    @Comment("Is this the primary contact person for this specific unit?")
    private boolean isPrimaryForUnit = false;

    @Column(nullable = false)
    @Comment("Is this a secondary contact for this specific unit?")
    private boolean isSecondaryForUnit = false;

    // Auditing – match your operation-service style
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

    @Column(nullable = false)
    @Comment("Active status flag")
    private boolean isActive = true;



    public void assignAsPrimaryToCompany(Company company) {
        this.company = company;
        this.isPrimaryForCompany = true;
        this.isSecondaryForCompany = false;

    }

    public void assignAsSecondaryToCompany(Company company) {
        this.company = company;
        this.isPrimaryForCompany = false;
        this.isSecondaryForCompany = true;
    }

    public void assignAsPrimaryToUnit(CompanyUnit unit) {
        this.companyUnit = unit;
        this.isPrimaryForUnit = true;
        this.isSecondaryForUnit = false;
    }

    public void assignAsSecondaryToUnit(CompanyUnit unit) {
        this.companyUnit = unit;
        this.isPrimaryForUnit = false;
        this.isSecondaryForUnit = true;
    }

    // Very useful utility method
    public boolean isCompanyLevel() {
        return company != null && (isPrimaryForCompany || isSecondaryForCompany);
    }

    public boolean isUnitLevel() {
        return companyUnit != null && (isPrimaryForUnit || isSecondaryForUnit);
    }

    public String getLevelDescription() {
        if (isPrimaryForCompany) return "Primary – Company";
        if (isSecondaryForCompany) return "Secondary – Company";
        if (isPrimaryForUnit) return "Primary – Unit " + (companyUnit != null ? companyUnit.getUnitName() : "");
        if (isSecondaryForUnit) return "Secondary – Unit " + (companyUnit != null ? companyUnit.getUnitName() : "");
        return "General contact";
    }
}