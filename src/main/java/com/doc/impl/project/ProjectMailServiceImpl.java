package com.doc.impl.project;

import com.doc.entity.client.Contact;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.project.Project;
import com.doc.repository.CompanyDocumentRepository;
import com.doc.repository.ProductDocumentMappingRepository;
import com.doc.service.ProjectMailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMailServiceImpl implements ProjectMailService {

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private final ProductDocumentMappingRepository productDocumentMappingRepository;
    private final CompanyDocumentRepository companyDocumentRepository;

    @Override
    public void sendProjectCreatedMail(Project project, Contact contact) {

        if (project == null || contact == null || !StringUtils.hasText(contact.getEmail())) {
            return;
        }

        if (project.getProduct() == null || project.getCompany() == null) {
            return;
        }

        List<ProductDocumentMapping> mappings =
                productDocumentMappingRepository.findByProductIdAndIsActiveTrue(project.getProduct().getId());

        if (mappings == null || mappings.isEmpty()) {
            log.info("No required documents mapped for productId: {}", project.getProduct().getId());
            sendProjectCreatedHtmlMail(project, contact, Collections.emptyList());
            return;
        }

        Map<Long, ProductRequiredDocuments> requiredDocMap = new LinkedHashMap<>();

        for (ProductDocumentMapping mapping : mappings) {
            if (mapping.getRequiredDocument() != null
                    && mapping.getRequiredDocument().isActive()
                    && !mapping.getRequiredDocument().isDeleted()) {

                requiredDocMap.putIfAbsent(
                        mapping.getRequiredDocument().getId(),
                        mapping.getRequiredDocument()
                );
            }
        }

        if (requiredDocMap.isEmpty()) {
            sendProjectCreatedHtmlMail(project, contact, Collections.emptyList());
            return;
        }

        List<Long> requiredDocumentIds = new ArrayList<>(requiredDocMap.keySet());

        List<Long> alreadyAvailableDocIds =
                companyDocumentRepository.findReusableRequiredDocumentIdsByCompany(
                        project.getCompany().getId(),
                        requiredDocumentIds
                );

        Set<Long> alreadyAvailableSet = new HashSet<>(
                alreadyAvailableDocIds != null ? alreadyAvailableDocIds : Collections.emptyList()
        );

        List<ProductRequiredDocuments> pendingDocuments =
                requiredDocMap.values()
                        .stream()
                        .filter(doc -> !alreadyAvailableSet.contains(doc.getId()))
                        .collect(Collectors.toList());

        if (pendingDocuments.isEmpty()) {
            log.info("All required documents are already available for companyId: {}", project.getCompany().getId());
        }

        sendProjectCreatedHtmlMail(project, contact, pendingDocuments);
    }

    private void sendProjectCreatedHtmlMail(
            Project project,
            Contact contact,
            List<ProductRequiredDocuments> pendingDocuments
    ) {
        try {
            String clientEmail = contact.getEmail().split("[,;]")[0].trim();

            if (!StringUtils.hasText(clientEmail)) {
                return;
            }

            Context context = new Context();
            context.setVariable("clientName", StringUtils.hasText(contact.getName()) ? contact.getName() : "Client");
            context.setVariable("projectNo", project.getProjectNo());
            context.setVariable("projectName", project.getName());
            context.setVariable("productName", project.getProduct().getProductName());
            context.setVariable("pendingDocuments", pendingDocuments);
            context.setVariable("hasPendingDocuments", pendingDocuments != null && !pendingDocuments.isEmpty());

            String htmlContent = templateEngine.process("mail/project-created-mail", context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom("erp@corpseed.com");
            helper.setTo(clientEmail);

            if (pendingDocuments != null && !pendingDocuments.isEmpty()) {
                helper.setSubject("Required Documents for Project - " + project.getProjectNo());
            } else {
                helper.setSubject("Project Created - " + project.getProjectNo());
            }

            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

            log.info("Project created HTML mail sent successfully to: {}", clientEmail);

        } catch (Exception e) {
            log.error("Failed to send project created HTML mail for projectId: {}",
                    project.getId(), e);
        }
    }
}