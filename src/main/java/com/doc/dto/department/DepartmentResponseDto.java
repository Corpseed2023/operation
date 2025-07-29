package com.doc.dto.department;


import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DepartmentResponseDto {
    private Long id;
    private String name;
    private Date createdDate;
    private Date updatedDate;


}