package com.doc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LeadDTO {
    private String uuid;
    private String name;
    private String leadName;
    private String originalName;
    private String email;
    private String leadDescription;
    private String mobileNo;
    private String urls;
    private Date createDate;
    private Date lastUpdated;
    private Date latestStatusChangeDate;
    private String source;
    private String PrimaryAddress;
    private boolean isDeleted;
    private String city;
    private Long createdById;
    private Long productId;
    private Long parentId;
    private String categoryId;
    private String serviceId;
    private String industryId;
    private String ipAddress;
    private String displayStatus;
    private Long assigneeId;
    private Boolean auto;
    private int count = 0;
    private int whatsAppStatus;
    private Long companyId;
    private String Address;
    private String City;
    private String State;
    private String Country;
    private String pinCode;
    private Long industriesId;
    private Long subIndustryId;
    private Long subsubIndustryId;
    private List<Long> industriesDataId;
}