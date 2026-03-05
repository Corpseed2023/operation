package com.doc;

import com.doc.entity.document.DocumentStatus;
import com.doc.entity.milestone.MilestoneStatus;
import com.doc.entity.project.ProjectStatus;
import com.doc.entity.client.PaymentType;
import com.doc.repository.MilestoneStatusRepository;
import com.doc.repository.documentRepo.DocumentStatusRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
import com.doc.repository.PaymentTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.util.Date;

/**
 * Main application class for initializing the application and seeding initial data.
 */
@SpringBootApplication
@EnableFeignClients
public class DocumentationApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentationApplication.class, args);
	}

	/**
	 * Initializes predefined statuses and payment types if the respective tables are empty.
	 * Uses explicit IDs to  match StatusConstants.java — NEVER CHANGE THESE IDs!
	 */
	//

	@Bean
	public CommandLineRunner initStatuses(MilestoneStatusRepository milestoneStatusRepository,
										  DocumentStatusRepository documentStatusRepository,
										  ProjectStatusRepository projectStatusRepository,
										  PaymentTypeRepository paymentTypeRepository) {
		return args -> {

			// === MILESTONE STATUSES ===
			if (milestoneStatusRepository.count() == 0) {
				createMilestoneStatus(milestoneStatusRepository, 1L, "NEW", "Initial state after becoming visible");
				createMilestoneStatus(milestoneStatusRepository, 2L, "IN_PROGRESS", "User has started working (e.g., calling client)");
				createMilestoneStatus(milestoneStatusRepository, 3L, "COMPLETED", "Milestone finished (e.g., documents verified)");
				createMilestoneStatus(milestoneStatusRepository, 4L, "REWORK", "Milestone sent back for correction");
				createMilestoneStatus(milestoneStatusRepository, 5L, "ON_HOLD", "Milestone paused (e.g., awaiting client response)");
				createMilestoneStatus(milestoneStatusRepository, 6L, "QUEUED", "Milestone queued for manual assignment");
				createMilestoneStatus(milestoneStatusRepository, 7L, "REJECTED", "Milestone permanently rejected");
			}

			// === DOCUMENT STATUSES ===
			if (documentStatusRepository.count() == 0) {
				createDocumentStatus(documentStatusRepository, 1L, "PENDING", "Document not yet uploaded");
				createDocumentStatus(documentStatusRepository, 2L, "UPLOADED", "Document uploaded, awaiting verification");
				createDocumentStatus(documentStatusRepository, 3L, "VERIFIED", "Document verified and approved");
				createDocumentStatus(documentStatusRepository, 4L, "REJECTED", "Document rejected (requires remarks)");
			}

			// === PROJECT STATUSES ===
			if (projectStatusRepository.count() == 0) {
				createProjectStatus(projectStatusRepository, 1L, "OPEN", "Initial state, no milestones started");
				createProjectStatus(projectStatusRepository, 2L, "IN_PROGRESS", "At least one milestone visible and in progress");
				createProjectStatus(projectStatusRepository, 3L, "COMPLETED", "All milestones completed");
				createProjectStatus(projectStatusRepository, 4L, "CANCELLED", "Project cancelled (e.g., client request)");
				createProjectStatus(projectStatusRepository, 5L, "REFUNDED", "Project refunded (reverts visibility)");
			}

			// === PAYMENT TYPES ===
			if (paymentTypeRepository.count() == 0) {
				createPaymentType(paymentTypeRepository, 1L, "FULL", "Full Payment");
				createPaymentType(paymentTypeRepository, 2L, "PARTIAL", "Partial Payment (50%)");
				createPaymentType(paymentTypeRepository, 3L, "INSTALLMENT", "Installment Payment");
				createPaymentType(paymentTypeRepository, 4L, "PURCHASE_ORDER", "Purchase Order Payment");
			}
		};
	}



	private void createMilestoneStatus(MilestoneStatusRepository repo, Long id, String name, String description) {
		MilestoneStatus status = new MilestoneStatus();
		status.setId(id);
		status.setName(name);
		status.setDescription(description);

		repo.save(status);
	}

	private void createDocumentStatus(DocumentStatusRepository repo, Long id, String name, String description) {
		DocumentStatus status = new DocumentStatus();
		status.setId(id);
		status.setName(name);
		status.setDescription(description);

		repo.save(status);
	}

	private void createProjectStatus(ProjectStatusRepository repo, Long id, String name, String description) {
		ProjectStatus status = new ProjectStatus();
		status.setId(id);
		status.setName(name);
		status.setDescription(description);

		repo.save(status);
	}

	private void createPaymentType(PaymentTypeRepository repo, Long id, String shortName, String fullName) {
		PaymentType paymentType = new PaymentType();
		paymentType.setId(id);
		paymentType.setName(fullName);
		paymentType.setCreatedDate(new Date());
		paymentType.setUpdatedDate(new Date());
		paymentType.setDeleted(false);
		paymentType.setDate(LocalDate.now());
		repo.save(paymentType);
	}
}