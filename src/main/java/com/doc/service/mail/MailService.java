package com.doc.service.mail;

import com.doc.dto.mail.MailRequestDto;

public interface MailService {

    void sendMail(MailRequestDto requestDto);
}