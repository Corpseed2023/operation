package com.doc.dto.contact;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactSummary {

    private Long contactId;
    private String name;
    private String designation;
    private boolean isPrimary;
    private boolean isSecondary;
}
