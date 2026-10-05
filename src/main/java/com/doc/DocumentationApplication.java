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
 * Main application class for initializing Operation Service.
 */
@SpringBootApplication
@EnableFeignClients
// @EnableScheduling
public class DocumentationApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentationApplication.class, args);
	}

	/**
	 * Initializes predefined statuses and payment types.
	 *
	 * IMPORTANT:
	 * Explicit IDs are being used because existing ERP code may
	 * depend on these IDs.
	 *
	 * Do not change existing IDs after production deployment.
	 */
	@Bean
	public CommandLineRunner initStatuses(
			MilestoneStatusRepository milestoneStatusRepository,
			DocumentStatusRepository documentStatusRepository,
			ProjectStatusRepository projectStatusRepository,
			PaymentTypeRepository paymentTypeRepository
	) {

		return args -> {

			// =========================================================
			// MILESTONE STATUSES
			// =========================================================

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


			// =========================================================
			// DOCUMENT STATUSES
			// =========================================================

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


			// =========================================================
			// PROJECT STATUSES
			// =========================================================

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

			createProjectStatusIfMissing(
					projectStatusRepository,
					7L,
					"FORCE_CLOSED",
					"Project force-closed after ADMIN approval on CRT request"
			);


			// =========================================================
			// PAYMENT TYPES
			// =========================================================
			//
			// IMPORTANT:
			// Keep these codes aligned with Account Service.
			//
			// Account Service:
			// FULL
			// PARTIAL
			// INSTALLMENT
			// PURCHASE_ORDER
			//

			createPaymentTypeIfMissing(
					paymentTypeRepository,
					1L,
					"FULL",
					"Full Payment"
			);

			createPaymentTypeIfMissing(
					paymentTypeRepository,
					2L,
					"PARTIAL",
					"Partial Payment"
			);

			createPaymentTypeIfMissing(
					paymentTypeRepository,
					3L,
					"INSTALLMENT",
					"Installment / Milestone Payment"
			);

			createPaymentTypeIfMissing(
					paymentTypeRepository,
					4L,
					"PURCHASE_ORDER",
					"Purchase Order Payment"
			);
		};
	}


	// =========================================================
	// MILESTONE STATUS
	// =========================================================

	private void createMilestoneStatusIfMissing(
			MilestoneStatusRepository repo,
			Long id,
			String name,
			String description
	) {

		repo.findById(id)
				.orElseGet(() -> {

					MilestoneStatus status = new MilestoneStatus();

					status.setId(id);
					status.setName(name);
					status.setDescription(description);

					return repo.save(status);
				});
	}


	// =========================================================
	// DOCUMENT STATUS
	// =========================================================

	private void createDocumentStatusIfMissing(
			DocumentStatusRepository repo,
			Long id,
			String name,
			String description
	) {

		repo.findById(id)
				.orElseGet(() -> {

					DocumentStatus status = new DocumentStatus();

					status.setId(id);
					status.setName(name);
					status.setDescription(description);

					return repo.save(status);
				});
	}


	// =========================================================
	// PROJECT STATUS
	// =========================================================

	private void createProjectStatusIfMissing(
			ProjectStatusRepository repo,
			Long id,
			String name,
			String description
	) {

		repo.findById(id)
				.orElseGet(() -> {

					ProjectStatus status = new ProjectStatus();

					status.setId(id);
					status.setName(name);
					status.setDescription(description);

					return repo.save(status);
				});
	}


	// =========================================================
	// PAYMENT TYPE
	// =========================================================

	private void createPaymentTypeIfMissing(
			PaymentTypeRepository repo,
			Long id,
			String code,
			String name
	) {

		PaymentType paymentType = repo.findById(id)
				.orElse(null);

		/*
		 * Existing database record:
		 *
		 * Your current table already contains IDs 1-4 without code.
		 * Therefore update the existing record with code/name instead
		 * of only creating a new record.
		 */
		if (paymentType != null) {

			boolean changed = false;

			if (paymentType.getCode() == null
					|| !paymentType.getCode().equalsIgnoreCase(code)) {

				paymentType.setCode(code);
				changed = true;
			}

			if (paymentType.getName() == null
					|| !paymentType.getName().equals(name)) {

				paymentType.setName(name);
				changed = true;
			}

			if (paymentType.isDeleted()) {
				paymentType.setDeleted(false);
				changed = true;
			}

			if (changed) {
				paymentType.setUpdatedDate(new Date());
				repo.save(paymentType);
			}

			return;
		}

		/*
		 * New database installation.
		 */
		PaymentType newPaymentType = new PaymentType();

		newPaymentType.setId(id);
		newPaymentType.setCode(code);
		newPaymentType.setName(name);

		newPaymentType.setDeleted(false);

		newPaymentType.setCreatedDate(new Date());
		newPaymentType.setUpdatedDate(new Date());

		newPaymentType.setDate(LocalDate.now());

		repo.save(newPaymentType);
	}
}