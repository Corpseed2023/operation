package com.doc.entity.client;

import java.util.Date;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * Represents a registered company in the system.
 * Contains GST details, contact, address, and industry classification.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Company ID")
    private Long id;

    @Column(name = "cnm", nullable = false, length = 255)
    @Comment("Company Name")
    private String name;

    /** GST Type (e.g., Regular, Composition) */
    @Column(name = "gst_type", length = 30)
    @Comment("Company GST Registration Type")
    private String companyGstType;

    /** Business Structure (e.g., Pvt Ltd, LLP, Proprietorship) */
    @Column(name = "gst_btype", length = 30)
    @Comment("Business Type under GST")
    private String gstBusinessType;

    @Column(name = "gstin", length = 15)
    @Comment("Company GSTIN")
    private String gstNo;

    @Temporal(TemporalType.DATE)
    @Column(name = "estd")
    @Comment("Establishment Date")
    private Date establishDate;

    @Column(name = "ind", length = 100)
    @Comment("Top-level Industry Name")
    private String industry;

    // Address block
    @Column(name = "ads", length = 1000)
    @Comment("Primary Address")
    private String address;

    @Column(name = "cty", length = 100)
    @Comment("City")
    private String city;

    @Column(name = "st", length = 100)
    @Comment("State")
    private String state;

    @Column(name = "cnt", length = 100)
    @Comment("Country")
    private String country;

    @Column(name = "ppc", length = 10)
    @Comment("Primary Pincode")
    private String primaryPinCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pcid")
    @Comment("Primary Contact for the Company")
    private Contact primaryContact;

    @Column(name = "inds", length = 100)
    @Comment("Mapped Industry Name or Code")
    private String industries;

    @Column(name = "subi", length = 100)
    @Comment("Sub-Industry Name or Code")
    private String subIndustry;

    @Column(name = "subsubi", length = 100)
    @Comment("Sub-Sub Industry Name or Code")
    private String subSubIndustry;
}