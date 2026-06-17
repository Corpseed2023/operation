package com.doc.impl.project;

import com.doc.entity.client.Contact;
import com.doc.entity.document.ProductDocumentMapping;
import com.doc.entity.document.ProductRequiredDocuments;
import com.doc.entity.project.Project;
import com.doc.repository.CompanyDocumentRepository;
import com.doc.repository.ProductDocumentMappingRepository;
import com.doc.service.ProjectMailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMailServiceImpl implements ProjectMailService {

    private final JavaMailSender javaMailSender;
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
            return;
        }

        List<Long> requiredDocumentIds = new ArrayList<>(requiredDocMap.keySet());

        List<Long> alreadyAvailableDocIds =
                companyDocumentRepository.findReusableRequiredDocumentIdsByCompany(
                        project.getCompany().getId(),
                        requiredDocumentIds
                );

        Set<Long> alreadyAvailableSet = new HashSet<>(alreadyAvailableDocIds);

        List<ProductRequiredDocuments> pendingDocuments =
                requiredDocMap.values()
                        .stream()
                        .filter(doc -> !alreadyAvailableSet.contains(doc.getId()))
                        .collect(Collectors.toList());

        if (pendingDocuments.isEmpty()) {
            log.info("All required documents are already available for companyId: {}", project.getCompany().getId());
            sendProjectCreatedMailWithoutPendingDocs(project, contact);
            return;
        }

        sendProjectCreatedMailWithPendingDocs(project, contact, pendingDocuments);
    }

    private void sendProjectCreatedMailWithPendingDocs(
            Project project,
            Contact contact,
            List<ProductRequiredDocuments> pendingDocuments
    ) {
        String clientEmail = contact.getEmail().split("[,;]")[0].trim();

        StringBuilder documentList = new StringBuilder();

        for (int i = 0; i < pendingDocuments.size(); i++) {
            ProductRequiredDocuments doc = pendingDocuments.get(i);

            documentList.append(i + 1)
                    .append(". ")
                    .append(doc.getName());

            if (StringUtils.hasText(doc.getAllowedFormats())) {
                documentList.append(" (Allowed formats: ")
                        .append(doc.getAllowedFormats())
                        .append(")");
            }

            documentList.append("\n");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("erp@corpseed.com");
        message.setTo(clientEmail);
        message.setSubject("Required Documents for Project - " + project.getProjectNo());

        message.setText(
                "Dear " + contact.getName() + ",\n\n" +
                        "Your project has been created successfully.\n\n" +
                        "Project No: " + project.getProjectNo() + "\n" +
                        "Project Name: " + project.getName() + "\n" +
                        "Product: " + project.getProduct().getProductName() + "\n\n" +
                        "Please share the following pending documents:\n\n" +
                        documentList +
                        "\nNote: Documents already available and verified in our records are not included above.\n\n" +
                        "Regards,\n" +
                        "Corpseed Team"
        );

        javaMailSender.send(message);
    }

    private void sendProjectCreatedMailWithoutPendingDocs(Project project, Contact contact) {
        String clientEmail = contact.getEmail().split("[,;]")[0].trim();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("erp@corpseed.com");
        message.setTo(clientEmail);
        message.setSubject("Project Created - " + project.getProjectNo());

        message.setText(
                "Dear " + contact.getName() + ",\n\n" +
                        "Your project has been created successfully.\n\n" +
                        "Project No: " + project.getProjectNo() + "\n" +
                        "Project Name: " + project.getName() + "\n" +
                        "Product: " + project.getProduct().getProductName() + "\n\n" +
                        "All required documents are already available in our records.\n\n" +
                        "Regards,\n" +
                        "Corpseed Team"
        );

        javaMailSender.send(message);
    }
}