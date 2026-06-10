package com.doc.dto.desigantion;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MapDesignationDepartmentRequestDto {

    private Long departmentId;

    private List<Long> designationIds;
}