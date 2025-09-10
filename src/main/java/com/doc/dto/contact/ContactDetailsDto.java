package com.doc.dto.contact;

import lombok.*;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class ContactDetailsDto {
    private Long id;
    private String name;
    private String emails;
    private String contactNo;
    private String whatsappNo;
    private String designation;
}