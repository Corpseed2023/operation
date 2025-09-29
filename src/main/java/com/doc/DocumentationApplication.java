package com.doc;

import com.doc.entity.document.DocumentStatus;
import com.doc.entity.project.MilestoneStatus;
import com.doc.entity.project.ProjectStatus;
import com.doc.entity.client.PaymentType;
import com.doc.repository.MilestoneStatusRepository;
import com.doc.repository.documentRepo.DocumentStatusRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.repository.PaymentTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.util.Date;

@SpringBootApplication
public class DocumentationApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentationApplication.class, args);
	}

	@Bean
	public CommandLineRunner initStatuses(MilestoneStatusRepository milestoneStatusRepository,
										  DocumentStatusRepository documentStatusRepository,
										  ProjectStatusRepository projectStatusRepository,
										  PaymentTypeRepository paymentTypeRepository) {
		return args -> {
			// Populate milestone_statuses if empty
			if (milestoneStatusRepository.count() == 0) {
				createMilestoneStatus(milestoneStatusRepository, "NEW", "Initial state after becoming visible");
				createMilestoneStatus(milestoneStatusRepository, "IN_PROGRESS", "User has started working (e.g., calling client)");
				createMilestoneStatus(milestoneStatusRepository, "ON_HOLD", "Milestone paused (e.g., awaiting client response)");
				createMilestoneStatus(milestoneStatusRepository, "COMPLETED", "Milestone finished (e.g., documents verified)");
				createMilestoneStatus(milestoneStatusRepository, "REJECTED", "Milestone rejected");
			}

			// Populate document_statuses if empty
			if (documentStatusRepository.count() == 0) {
				createDocumentStatus(documentStatusRepository, "PENDING", "Document not yet uploaded");
				createDocumentStatus(documentStatusRepository, "UPLOADED", "Document uploaded, awaiting verification");
				createDocumentStatus(documentStatusRepository, "VERIFIED", "Document verified");
				createDocumentStatus(documentStatusRepository, "REJECTED", "Document rejected (requires remarks)");
			}

			// Populate project_statuses if empty
			if (projectStatusRepository.count() == 0) {
				createProjectStatus(projectStatusRepository, "OPEN", "Initial state, no milestones started");
				createProjectStatus(projectStatusRepository, "IN_PROGRESS", "At least one milestone visible and in progress");
				createProjectStatus(projectStatusRepository, "COMPLETED", "All milestones completed");
				createProjectStatus(projectStatusRepository, "CANCELLED", "Project cancelled (e.g., client request)");
				createProjectStatus(projectStatusRepository, "REFUNDED", "Project refunded (reverts visibility)");
			}

			// Populate payment_types if empty
			if (paymentTypeRepository.count() == 0) {
				createPaymentType(paymentTypeRepository, 1L, "FULL");
				createPaymentType(paymentTypeRepository, 2L, "PARTIAL");
				createPaymentType(paymentTypeRepository, 3L, "INSTALLMENT");
				createPaymentType(paymentTypeRepository, 4L, "PURCHASE_ORDER");
			}
		};
	}

	private void createMilestoneStatus(MilestoneStatusRepository repo, String name, String description) {
		MilestoneStatus status = new MilestoneStatus();
		status.setName(name);
		status.setDescription(description);
		repo.save(status);
	}

	private void createDocumentStatus(DocumentStatusRepository repo, String name, String description) {
		DocumentStatus status = new DocumentStatus();
		status.setName(name);
		status.setDescription(description);
		repo.save(status);
	}

	private void createProjectStatus(ProjectStatusRepository repo, String name, String description) {
		ProjectStatus status = new ProjectStatus();
		status.setName(name);
		status.setDescription(description);
		repo.save(status);
	}

	private void createPaymentType(PaymentTypeRepository repo, Long id, String name) {
		PaymentType paymentType = new PaymentType();
		paymentType.setId(id);
		paymentType.setName(name);
		paymentType.setCreatedDate(new Date());
		paymentType.setUpdatedDate(new Date());
		// createdBy and updatedBy left as null (system-initialized, no user required)
		paymentType.setDeleted(false);
		paymentType.setDate(LocalDate.now());
		repo.save(paymentType);
	}
}