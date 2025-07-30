package com.doc.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class PaymentTypeResponseDto {

    private Long id;

    private String name;

    private Date createdDate;

    private Long createdBy;

    private Date updatedDate;

    private Long updatedBy;
}
