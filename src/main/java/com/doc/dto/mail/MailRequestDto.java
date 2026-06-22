package com.doc.dto.mail;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailRequestDto {

    private String to;

    private List<String> cc;

    private List<String> bcc;

    private String subject;

    private String body;

    private boolean html;
}