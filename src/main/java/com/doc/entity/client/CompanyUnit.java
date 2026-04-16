package com.doc.entity.client;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "company_unit",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"company_id", "unit_name"}),
                @UniqueConstraint(columnNames = {"company_id", "gst_no"})
        })
@Getter
@Setter
public class CompanyUnit {

    @Id
    @Comment("Primary key: Unit / Branch ID")
    private Long id;

    @Column(name = "unit_name", nullable = false, length = 150)
    @Comment("Unit / Branch / Outlet name (e.g. Noida Sector 62, Lucknow Gomti Nagar)")
    private String unitName;

    @Column(name = "address", length = 1000, nullable = false)
    @Comment("Full address of this branch / outlet")
    private String address;

    @Column(name = "city", length = 100, nullable = false)
    @Comment("City")
    private String city;

    @Column(name = "state", length = 100, nullable = false)
    @Comment("State / UT")
    private String state;

    @Column(name = "country", length = 100, nullable = false)
    @Comment("Country")
    private String country = "India";

    @Column(name = "pin_code", length = 10, nullable = false)
    @Comment("PIN / ZIP code")
    private String pinCode;

    @Column(name = "gst_no", length = 15)
    @Comment("GSTIN of this specific unit / branch")
    private String gstNo;

    @Column(nullable = false, length = 50)
    @Comment("Current status of the unit")
    private String status = "Active";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    @Comment("Parent company this unit belongs to")
    private Company company;

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

    // Unit-specific contacts
    @OneToMany(mappedBy = "companyUnit", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("Contacts specific to this unit / branch")
    private List<Contact> unitContacts = new ArrayList<>();

}