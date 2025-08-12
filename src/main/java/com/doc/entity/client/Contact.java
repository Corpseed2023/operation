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

    @Comment("Contact Title")
    private String title;

    @Column(nullable = false)
    @Comment("Contact Name")
    private String name;

    @Column(length = 500)
    @Comment("Contact Emails")
    private String emails;

    @Column(length = 20)
    @Comment("Contact Mobile Number")
    private String contactNo;

    @Column(length = 20)
    @Comment("Contact WhatsApp Number")
    private String whatsappNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    @Comment("Company associated with the contact")
    private Company company;

    @Comment("Contact Designation")
    private String designation;

    @Column(nullable = false)
    @Comment("Soft delete flag")
    private boolean deleteStatus = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    @Comment("Creation Date")
    private Date createdDate;

    @Comment("Created By User ID")
    private Long createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Comment("Update Date")
    private Date updatedDate;

    @Comment("Updated By User ID")
    private Long updatedBy;
}