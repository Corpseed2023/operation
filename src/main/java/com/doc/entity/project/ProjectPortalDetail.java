package com.doc.entity.project;

import com.doc.entity.client.Company;
import com.doc.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name = "project_portal_details",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "portal_name"}))
@Getter @Setter @NoArgsConstructor
public class ProjectPortalDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @Comment("Which project this portal login belongs to")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @Comment("Which company (client) this portal belongs to")
    private Company company;

    @Column(name = "portal_name", nullable = false, length = 255)
    @Comment("Name of the portal e.g. CPCB EPR Plastic Portal, FoSCoS, BIS CRS, Parivesh")
    private String portalName;

    @Column(name = "portal_url", length = 512)
    @Comment("Login URL of the portal (optional but recommended)")
    private String portalUrl;

    @Column(name = "username", nullable = false, length = 255)
    @Comment("Client login ID / username / email used for login")
    private String username;

    @Column(name = "password", nullable = false, length = 500)
    @Comment("Encrypted password - never store plain text")
    private String password;

    @Column(name = "remarks", length = 1000)
    @Comment("Extra note e.g. OTP on mobile, shared via WhatsApp")
    private String remarks;

    @Column(name = "date")
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @Comment("User who added these portal details")
    private User createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date", updatable = false)
    @Comment("When these details were added")
    private Date createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    @Comment("Last user who updated these details")
    private User updatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    @Comment("Last update timestamp")
    private Date updatedDate;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        createdDate = new Date();
        updatedDate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = new Date();
    }
}