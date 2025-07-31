package com.doc.entity.client;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Contact {



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Primary key: Contact ID")
    private Long id;

    @Column(name = "ttl")
    @Comment("Contact Title")
    private String title;

    @Column(name = "nm", nullable = false)
    @Comment("Contact Name")
    private String name;

    @Column(name = "em", length = 500)
    @Comment("Contact Emails")
    private String emails;

    @Column(name = "mob", length = 20)
    @Comment("Contact Mobile Number")
    private String contactNo;

    @Column(name = "wapp", length = 20)
    @Comment("Contact WhatsApp Number")
    private String whatsappNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cmpid")
    @Comment("Company associated with the contact")
    private Company company;

    @Column(name = "desg")
    @Comment("Contact Designation")
    private String designation;

    @Column(name = "isd", nullable = false)
    @Comment("Soft delete flag")
    private boolean deleteStatus = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdt", updatable = false)
    @Comment("Creation Date")
    private Date createdDate;

    @Column(name = "cb")
    @Comment("Created By User ID")
    private Long createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "udt")
    @Comment("Update Date")
    private Date updatedDate;

    @Column(name = "ub")
    @Comment("Updated By User ID")
    private Long updatedBy;
}