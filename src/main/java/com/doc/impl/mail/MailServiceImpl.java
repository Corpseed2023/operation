package com.doc.impl.mail;

import com.doc.dto.mail.MailRequestDto;
import com.doc.service.mail.MailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromMail;

    @Override
    public void sendMail(MailRequestDto requestDto) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    true,
                    "UTF-8"
            );

            helper.setFrom(fromMail);
            helper.setTo(requestDto.getTo());
            helper.setSubject(requestDto.getSubject());
            helper.setText(requestDto.getBody(), requestDto.isHtml());

            if (requestDto.getCc() != null && !requestDto.getCc().isEmpty()) {
                helper.setCc(requestDto.getCc().toArray(new String[0]));
            }

            if (requestDto.getBcc() != null && !requestDto.getBcc().isEmpty()) {
                helper.setBcc(requestDto.getBcc().toArray(new String[0]));
            }

            javaMailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send mail: " + e.getMessage(), e);
        }
    }
}