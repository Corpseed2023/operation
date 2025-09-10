package com.doc.dto.project;

import lombok.*;

import java.time.LocalDate;
import java.util.Date;

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
}