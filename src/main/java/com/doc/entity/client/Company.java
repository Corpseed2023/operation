package com.doc.entity.client;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Company {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Company ID")
    private Long id;

    @Column(nullable = false, length = 255)
    @Comment("Company Name")
    private String name;

    @Column(name = "gst_type", length = 30)
    @Comment("Company GST Registration Type")
    private String companyGstType;

    @Column(name = "gst_btype", length = 30)
    @Comment("Business Type under GST")
    private String gstBusinessType;

    @Column(name = "gstin", length = 15)
    @Comment("Company GSTIN")
    private String gstNo;

    @Temporal(TemporalType.DATE)
    @Comment("Establishment Date")
    private Date establishDate;

    @Column(length = 100)
    @Comment("Top-level Industry Name")
    private String industry;

    @Column(length = 1000)
    @Comment("Primary Address")
    private String address;

    @Column(name = "cty", length = 100)
    @Comment("City")
    private String city;

    @Column(length = 100)
    @Comment("State")
    private String state;

    @Column(length = 100)
    @Comment("Country")
    private String country;

    @Column(length = 10)
    @Comment("Primary Pincode")
    private String primaryPinCode;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("List of contacts associated with the company")
    private List<Contact> contacts = new ArrayList<>();

    @Column(length = 100)
    @Comment("Mapped Industry Name or Code")
    private String industries;

    @Column(length = 100)
    @Comment("Sub-Industry Name or Code")
    private String subIndustry;

    @Column(length = 100)
    @Comment("Sub-Sub Industry Name or Code")
    private String subSubIndustry;

    @Column(name = "is_deleted", nullable = false)
    @Comment("Soft delete flag")
    private boolean isDeleted = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_date")
    @Comment("Creation Date")
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_date")
    @Comment("Update Date")
    private Date updatedDate;
}