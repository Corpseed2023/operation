package com.doc.dto.role;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
public class RoleResponseDto {
    private Long id;
    private String name;
    private Date createdDate;
    private Date updatedDate;
    private boolean isDeleted;
}