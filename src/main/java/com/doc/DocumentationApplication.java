package com.doc;

import com.doc.entity.client.PaymentType;
import com.doc.entity.document.DocumentStatus;
import com.doc.entity.milestone.MilestoneStatus;
import com.doc.entity.project.ProjectStatus;
import com.doc.repository.MilestoneStatusRepository;
import com.doc.repository.PaymentTypeRepository;
import com.doc.repository.documentRepo.DocumentStatusRepository;
import com.doc.repository.projectRepo.ProjectStatusRepository;
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
	 * Initializes predefined statuses and payment types.
	 *
	 * IMPORTANT:
	 * Uses explicit IDs to match StatusConstants.java.
	 * NEVER CHANGE THESE IDs.
	 *
	 * This uses create-if-missing instead of count() == 0,
	 * because later when we add a new status, existing DB should also receive it.
	 */
	@Bean
	public CommandLineRunner initStatuses(
			MilestoneStatusRepository milestoneStatusRepository,
			DocumentStatusRepository documentStatusRepository,
			ProjectStatusRepository projectStatusRepository,
			PaymentTypeRepository paymentTypeRepository
	) {
		return args -> {

			// === MILESTONE STATUSES ===
			createMilestoneStatusIfMissing(
					milestoneStatusRepository,
					1L,
					"NEW",
					"Initial state after becoming visible"
			);

			createMilestoneStatusIfMissing(
					milestoneStatusRepository,
					2L,
					"IN_PROGRESS",
					"User has started working"
			);

			createMilestoneStatusIfMissing(
					milestoneStatusRepository,
					3L,
					"COMPLETED",
					"Milestone finished"
			);

			createMilestoneStatusIfMissing(
					milestoneStatusRepository,
					4L,
					"REWORK",
					"Milestone sent back for correction"
			);

			createMilestoneStatusIfMissing(
					milestoneStatusRepository,
					5L,
					"ON_HOLD",
					"Milestone paused"
			);

			createMilestoneStatusIfMissing(
					milestoneStatusRepository,
					6L,
					"QUEUED",
					"Milestone queued for manual assignment"
			);

			createMilestoneStatusIfMissing(
					milestoneStatusRepository,
					7L,
					"REJECTED",
					"Milestone permanently rejected"
			);

			// === DOCUMENT STATUSES ===
			createDocumentStatusIfMissing(
					documentStatusRepository,
					1L,
					"PENDING",
					"Document not yet uploaded"
			);

			createDocumentStatusIfMissing(
					documentStatusRepository,
					2L,
					"UPLOADED",
					"Document uploaded, awaiting verification"
			);

			createDocumentStatusIfMissing(
					documentStatusRepository,
					3L,
					"VERIFIED",
					"Document verified and approved"
			);

			createDocumentStatusIfMissing(
					documentStatusRepository,
					4L,
					"REJECTED",
					"Document rejected"
			);

			// === PROJECT STATUSES ===
			createProjectStatusIfMissing(
					projectStatusRepository,
					1L,
					"OPEN",
					"Initial state, no milestones started"
			);

			createProjectStatusIfMissing(
					projectStatusRepository,
					2L,
					"IN_PROGRESS",
					"At least one milestone visible and in progress"
			);

			createProjectStatusIfMissing(
					projectStatusRepository,
					3L,
					"COMPLETED",
					"All milestones completed"
			);

			createProjectStatusIfMissing(
					projectStatusRepository,
					4L,
					"CANCELLED",
					"Project cancelled"
			);

			createProjectStatusIfMissing(
					projectStatusRepository,
					5L,
					"REFUNDED",
					"Project refunded"
			);

			createProjectStatusIfMissing(
					projectStatusRepository,
					6L,
					"REOPENED",
					"Project reopened after manager approval due to mistake"
			);

			// === PAYMENT TYPES ===
			createPaymentTypeIfMissing(
					paymentTypeRepository,
					1L,
					"Full Payment"
			);

			createPaymentTypeIfMissing(
					paymentTypeRepository,
					2L,
					"Partial Payment (50%)"
			);

			createPaymentTypeIfMissing(
					paymentTypeRepository,
					3L,
					"Installment Payment"
			);

			createPaymentTypeIfMissing(
					paymentTypeRepository,
					4L,
					"Purchase Order Payment"
			);
		};
	}

	private void createMilestoneStatusIfMissing(
			MilestoneStatusRepository repo,
			Long id,
			String name,
			String description
	) {
		repo.findById(id).orElseGet(() -> {
			MilestoneStatus status = new MilestoneStatus();
			status.setId(id);
			status.setName(name);
			status.setDescription(description);
			return repo.save(status);
		});
	}

	private void createDocumentStatusIfMissing(
			DocumentStatusRepository repo,
			Long id,
			String name,
			String description
	) {
		repo.findById(id).orElseGet(() -> {
			DocumentStatus status = new DocumentStatus();
			status.setId(id);
			status.setName(name);
			status.setDescription(description);
			return repo.save(status);
		});
	}

	private void createProjectStatusIfMissing(
			ProjectStatusRepository repo,
			Long id,
			String name,
			String description
	) {
		repo.findById(id).orElseGet(() -> {
			ProjectStatus status = new ProjectStatus();
			status.setId(id);
			status.setName(name);
			status.setDescription(description);
			return repo.save(status);
		});
	}

	private void createPaymentTypeIfMissing(
			PaymentTypeRepository repo,
			Long id,
			String name
	) {
		repo.findById(id).orElseGet(() -> {
			PaymentType paymentType = new PaymentType();
			paymentType.setId(id);
			paymentType.setName(name);
			paymentType.setCreatedDate(new Date());
			paymentType.setUpdatedDate(new Date());
			paymentType.setDeleted(false);
			paymentType.setDate(LocalDate.now());
			return repo.save(paymentType);
		});
	}
}