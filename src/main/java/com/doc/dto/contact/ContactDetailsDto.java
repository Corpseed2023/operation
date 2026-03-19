package com.doc.dto.contact;

import lombok.*;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@Builder
public class ContactDetailsDto {

    private Long id;

    private String title;                  // Mr/Ms/Dr etc.

    private String name;                   // Full name (required)

    private String designation;            // Our internal designation

    private String clientDesignation;      // How client refers to this person

    // Contact info (may be masked for non-admin users)
    private String emails;                 // comma-separated

    private String contactNo;

    private String whatsappNo;

    // Level / context information
    private String level;                  // "Company" or "Unit"

    private String unitName;               // Only populated if level = "Unit"

    private String levelDescription;       // e.g. "Primary – Unit Noida Sector 62"

    // Flags (useful for UI sorting / highlighting)
    private boolean isPrimary;

    private boolean isSecondary;

    // Optional: audit / status (if needed in response)
    private boolean isActive;

    // You can add more if needed, e.g.:
    // private Date createdDate;
    // private String createdByName;
}