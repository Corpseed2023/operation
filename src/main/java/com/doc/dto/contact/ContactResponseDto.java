package com.doc.dto.contact;


import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ContactResponseDto {

    private Long id;

    private String title;

    private String name;

    private String emails;

    private String contactNo;

    private String whatsappNo;

    private Long companyId;

    private String designation;

    private Date createdDate;

    private Long createdBy;

    private Date updatedDate;

    private Long updatedBy;
}
