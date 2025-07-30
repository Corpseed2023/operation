package com.doc.dto.desigantion;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class DesignationResponseDto {

    private Long id;

    private String name;

    private Long weightValue;

    private Long departmentId;

    private String departmentName;

    private Date createdDate;

    private Date updatedDate;
}