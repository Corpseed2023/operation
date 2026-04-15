package com.doc.dto.LegalRequestDto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
public class LegalRequestDto {

    private Long id;
    private String projectName;
    private String milestoneName;
    private double tatInDays;
    private String status;

    private String assignedToName;
    private String createdByName;
    private String updatedByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String statusReason;


    private List<String> documents;
}
