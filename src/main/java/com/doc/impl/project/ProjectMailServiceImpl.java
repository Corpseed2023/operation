package com.doc.impl.project;

import com.doc.entity.client.Contact;
import com.doc.entity.project.Project;
import com.doc.service.ProjectMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProjectMailServiceImpl implements ProjectMailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Override
    public void sendProjectCreatedMail(Project project, Contact contact) {

        if (contact == null || !StringUtils.hasText(contact.getEmail())) {
            return;
        }

        String clientEmail = contact.getEmail().split("[,;]")[0].trim();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("erp@corpseed.com");
        message.setTo(clientEmail);
        message.setSubject("Project Created - " + project.getProjectNo());

        message.setText(
                "Dear " + contact.getName() + ",\n\n" +
                        "Your project has been created successfully.\n\n" +
                        "Project No: " + project.getProjectNo() + "\n" +
                        "Project Name: " + project.getProjectNo() + "\n\n" +
                        "Our team will proceed with the next steps shortly.\n\n" +
                        "Regards,\n" +
                        "Corpseed Team"
        );

        javaMailSender.send(message);
    }
}