package com.doc.entity.client;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;

/**
 * Represents a contact person for a company or project.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ttl")
    private String title;

    @Column(name = "nm", nullable = false)
    private String name;

    @Column(name = "em", length = 500)
    private String emails;

    @Column(name = "mob", length = 20)
    private String contactNo;

    @Column(name = "wapp", length = 20)
    private String whatsappNo;

    @Column(name = "cmpid")
    private Long companyId;

    @Column(name = "desg")
    private String designation;

    @Column(name = "isd", nullable = false)
    private boolean deleteStatus = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "cdt", updatable = false)
    private Date createdDate;

    @Column(name = "cb")
    private Long createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "udt")
    private Date updatedDate;

    @Column(name = "ub")
    private Long updatedBy;

}
