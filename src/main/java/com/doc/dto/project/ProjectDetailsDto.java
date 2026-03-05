package com.doc.dto.project;

import com.doc.dto.contact.ContactDetailsDto;
import lombok.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class ProjectDetailsDto {

    private Long id;
    private String name;
    private String projectNo;
    private LocalDate date;

    private Long productId;
    private String productName;

    // Sales Person
    private Long salesPersonId;
    private String salesPersonName;          // NEW

    private Long companyId;
    private String companyName;              // NEW

    private Long contactId;
    private String contactName;              // NEW

    private List<ContactDetailsDto> contacts;

    private Date createdDate;
    private Date updatedDate;

    private Long applicantId;
    private String applicantName;
}