package com.doc.dto.LegalRequestDto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LegalRequestStatusCountDto {
    private String status;
    private long count;
}