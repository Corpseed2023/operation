package com.doc.dto.project;

import lombok.Data;
import java.time.LocalDate;
import java.util.Date;

@Data
public class ProjectDetailsDto {
    private Long id;
    private String name;
    private String projectNo;
    private LocalDate date;
    private String address;
    private String city;
    private String state;
    private String country;
    private Date createdDate;
    private Date updatedDate;
}
