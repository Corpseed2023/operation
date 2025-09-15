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
    private String address;
    private String city;
    private String state;
    private String country;
    private Long productId;
    private String productName;
    private Date createdDate;
    private Date updatedDate;
    private Long companyId;
    private String companyName;
    private List<ContactDetailsDto> contacts;
}