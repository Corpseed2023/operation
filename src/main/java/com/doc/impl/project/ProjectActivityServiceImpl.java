package com.doc.impl.project;

import com.doc.dto.project.activity.CreateCommentRequestDto;
import com.doc.dto.project.activity.CreateNoteRequestDto;
import com.doc.dto.project.activity.ProjectActivityResponseDto;
import com.doc.dto.project.activity.ProjectCommentResponseDto;
import com.doc.dto.project.activity.expense.AccountsExpenseDecisionRequestDto;
import com.doc.dto.project.activity.expense.CreateExpenseRequestDto;
import com.doc.dto.project.activity.expense.CrtExpenseDecisionRequestDto;
import com.doc.dto.project.activity.expense.GovernmentFeeFundTransferRequestDto;
import com.doc.dto.project.activity.expense.ProjectExpenseResponseDto;
import com.doc.em.ActivityType;
import com.doc.em.AccountPostingStatus;
import com.doc.em.ApprovalStatus;
import com.doc.em.ExpenseApprovalStage;
import com.doc.em.ExpenseCategory;
import com.doc.em.ExpensePaidBy;
import com.doc.em.ExpensePaymentStatus;
import com.doc.entity.department.Department;
import com.doc.entity.project.Project;
import com.doc.entity.project.ProjectActivity;
import com.doc.entity.project.activity.ProjectComment;
import com.doc.entity.project.activity.ProjectExpense;
import com.doc.entity.project.activity.ProjectNote;
import com.doc.entity.user.Role;
import com.doc.entity.user.User;
import com.doc.exception.ResourceNotFoundException;
import com.doc.exception.ValidationException;
import com.doc.repository.ProjectRepository;
import com.doc.repository.UserRepository;
import com.doc.repository.projectRepo.activity.ProjectActivityRepository;
import com.doc.repository.projectRepo.activity.ProjectCommentRepository;
import com.doc.repository.projectRepo.activity.ProjectExpenseRepository;
import com.doc.repository.projectRepo.activity.ProjectNoteRepository;
import com.doc.service.ExpenseAccountPostingService;
import com.doc.service.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectActivityServiceImpl implements ProjectActivityService {

    private static final Set<ApprovalStatus> ALLOWED_DECISION_STATUSES =
            EnumSet.of(
                    ApprovalStatus.APPROVED,
                    ApprovalStatus.REJECTED,
                    ApprovalStatus.ON_HOLD
            );

    private static final List<ExpensePaymentStatus> ACTIVE_PAYMENT_STATUSES =
            List.of(
                    ExpensePaymentStatus.PENDING,
                    ExpensePaymentStatus.PROCESSING,
                    ExpensePaymentStatus.PARTIALLY_PAID,
                    ExpensePaymentStatus.FAILED
            );

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectActivityRepository activityRepository;
    private final ProjectNoteRepository noteRepository;
    private final ProjectCommentRepository commentRepository;
    private final ProjectExpenseRepository expenseRepository;

    private final ExpenseAccountPostingService
            expenseAccountPostingService;

    private static final Set<String> ALLOWED_CLIENT_PAYMENT_MODES =
            Set.of(
                    "CASH",
                    "CASH_DEPOSIT",
                    "CHEQUE",
                    "DEMAND_DRAFT",
                    "NEFT",
                    "RTGS",
                    "IMPS",
                    "UPI",
                    "CARD",
                    "BANK_TRANSFER",
                    "OTHER"
            );

    private static final Set<String> BANK_DETAILS_REQUIRED_PAYMENT_MODES =
            Set.of(
                    "CASH_DEPOSIT",
                    "CHEQUE",
                    "DEMAND_DRAFT",
                    "NEFT",
                    "RTGS",
                    "IMPS",
                    "UPI",
                    "CARD",
                    "BANK_TRANSFER"
            );

    // =========================================================
    // NOTE
    // =========================================================

    @Override
    @Transactional
    public ProjectActivityResponseDto addNote(
            Long projectId,
            CreateNoteRequestDto request
    ) {

        log.info(
                "[NOTE-CREATE-START] projectId={} | createdByUserId={}",
                projectId,
                request != null ? request.getCreatedByUserId() : null
        );

        if (request == null) {
            log.warn(
                    "[NOTE-CREATE-VALIDATION-FAILED] projectId={} | reason=request-null",
                    projectId
            );
            throw new ValidationException(
                    "Note request is required",
                    "ERR_NOTE_REQUEST_REQUIRED"
            );
        }

        if (request.getCreatedByUserId() == null) {
            log.warn(
                    "[NOTE-CREATE-VALIDATION-FAILED] projectId={} | reason=created-by-user-id-null",
                    projectId
            );
            throw new ValidationException(
                    "Created by user ID is required",
                    "ERR_CREATED_BY_REQUIRED"
            );
        }

        if (request.getNoteText() == null ||
                request.getNoteText().trim().isEmpty()) {

            log.warn(
                    "[NOTE-CREATE-VALIDATION-FAILED] projectId={} | userId={} | reason=note-text-empty",
                    projectId,
                    request.getCreatedByUserId()
            );
            throw new ValidationException(
                    "Note text is required",
                    "ERR_NOTE_TEXT_REQUIRED"
            );
        }

        User user = validateActiveUser(request.getCreatedByUserId());
        Project project = validateActiveProject(projectId);

        String noteText = request.getNoteText().trim();

        ProjectActivity activity = createActivity(
                project,
                ActivityType.NOTE,
                "Note Added",
                noteText,
                user,
                false
        );

        activity = activityRepository.save(activity);

        log.debug(
                "[NOTE-ACTIVITY-SAVED] projectId={} | activityId={} | userId={}",
                projectId,
                activity.getId(),
                user.getId()
        );

        ProjectNote note = new ProjectNote();
        note.setProject(project);
        note.setActivity(activity);
        note.setNoteText(noteText);
        note.setCreatedDate(LocalDateTime.now());
        note.setCreatedByUserId(user.getId());
        note.setCreatedByUserName(user.getFullName());

        note = noteRepository.save(note);

        log.info(
                "[NOTE-CREATE-SUCCESS] projectId={} | activityId={} | noteId={} | userId={}",
                projectId,
                activity.getId(),
                note.getId(),
                user.getId()
        );

        return mapResponse(activity, note);
    }

    // =========================================================
    // COMMENT
    // =========================================================

    @Override
    @Transactional
    public ProjectActivityResponseDto addComment(
            Long projectId,
            CreateCommentRequestDto request
    ) {

        log.info(
                "[COMMENT-CREATE-START] projectId={} | createdByUserId={} | parentCommentId={}",
                projectId,
                request != null ? request.getCreatedByUserId() : null,
                request != null ? request.getParentCommentId() : null
        );

        if (request == null) {
            log.warn(
                    "[COMMENT-CREATE-VALIDATION-FAILED] projectId={} | reason=request-null",
                    projectId
            );
            throw new ValidationException(
                    "Comment request is required",
                    "ERR_COMMENT_REQUEST_REQUIRED"
            );
        }

        if (request.getCreatedByUserId() == null) {
            log.warn(
                    "[COMMENT-CREATE-VALIDATION-FAILED] projectId={} | reason=created-by-user-id-null",
                    projectId
            );
            throw new ValidationException(
                    "Created by user ID is required",
                    "ERR_CREATED_BY_REQUIRED"
            );
        }

        if (request.getCommentText() == null ||
                request.getCommentText().trim().isEmpty()) {

            log.warn(
                    "[COMMENT-CREATE-VALIDATION-FAILED] projectId={} | userId={} | reason=comment-text-empty",
                    projectId,
                    request.getCreatedByUserId()
            );
            throw new ValidationException(
                    "Comment text is required",
                    "ERR_COMMENT_TEXT_REQUIRED"
            );
        }

        User user = validateActiveUser(request.getCreatedByUserId());
        Project project = validateActiveProject(projectId);

        if (request.getParentCommentId() != null) {
            log.debug(
                    "[COMMENT-PARENT-VALIDATION] projectId={} | parentCommentId={}",
                    projectId,
                    request.getParentCommentId()
            );

            ProjectComment parentComment = commentRepository
                    .findById(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent comment not found",
                            "ERR_PARENT_COMMENT_NOT_FOUND"
                    ));

            if (!parentComment.getProject().getId().equals(projectId)) {
                log.warn(
                        "[COMMENT-PARENT-VALIDATION-FAILED] projectId={} | parentCommentId={} | parentProjectId={}",
                        projectId,
                        request.getParentCommentId(),
                        parentComment.getProject().getId()
                );
                throw new ValidationException(
                        "Parent comment does not belong to this project",
                        "ERR_INVALID_PARENT_COMMENT"
                );
            }
        }


        String commentText = request.getCommentText().trim();

        ProjectActivity activity = createActivity(
                project,
                ActivityType.COMMENT,
                "Comment Added",
                commentText,
                user,
                false
        );

        activity = activityRepository.save(activity);

        log.debug(
                "[COMMENT-ACTIVITY-SAVED] projectId={} | activityId={} | userId={}",
                projectId,
                activity.getId(),
                user.getId()
        );

        ProjectComment comment = new ProjectComment();
        comment.setProject(project);
        comment.setActivity(activity);
        comment.setCommentText(commentText);
        comment.setParentCommentId(request.getParentCommentId());
        comment.setCreatedDate(LocalDateTime.now());
        comment.setCreatedByUserId(user.getId());
        comment.setCreatedByUserName(user.getFullName());

        comment = commentRepository.save(comment);

        log.info(
                "[COMMENT-CREATE-SUCCESS] projectId={} | activityId={} | commentId={} | parentCommentId={} | userId={}",
                projectId,
                activity.getId(),
                comment.getId(),
                comment.getParentCommentId(),
                user.getId()
        );

        return mapResponse(activity, comment);
    }

    // =========================================================
    // EXPENSE CREATION
    // =========================================================

    @Override
    @Transactional
    public ProjectActivityResponseDto addExpense(
            Long projectId,
            CreateExpenseRequestDto request
    ) {

        log.info(
                "[EXPENSE-CREATE-START] projectId={} | createdByUserId={} | departmentId={} | category={}",
                projectId,
                request != null ? request.getCreatedByUserId() : null,
                request != null ? request.getDepartmentId() : null,
                request != null ? request.getExpenseCategory() : null
        );

        if (request == null) {
            log.warn(
                    "[EXPENSE-CREATE-VALIDATION-FAILED] projectId={} | reason=request-null",
                    projectId
            );
            throw new ValidationException(
                    "Expense request is required",
                    "ERR_EXPENSE_REQUEST_REQUIRED"
            );
        }

        if (request.getCreatedByUserId() == null) {
            log.warn(
                    "[EXPENSE-CREATE-VALIDATION-FAILED] projectId={} | reason=created-by-user-id-null",
                    projectId
            );
            throw new ValidationException(
                    "Created by user ID is required",
                    "ERR_CREATED_BY_REQUIRED"
            );
        }

        User user = validateActiveUser(request.getCreatedByUserId());
        Project project = validateActiveProject(projectId);

        Department department = validateUserDepartment(
                user,
                request.getDepartmentId()
        );

        validateGovernmentFeeRaiser(
                department,
                request.getExpenseCategory()
        );

        log.debug(
                "[EXPENSE-CREATE-CONTEXT-VALIDATED] projectId={} | userId={} | departmentId={}",
                project.getId(),
                user.getId(),
                department.getId()
        );

        if (request.getExpenseCategory() == null) {
            log.warn(
                    "[EXPENSE-CREATE-VALIDATION-FAILED] projectId={} | userId={} | reason=category-null",
                    projectId,
                    user.getId()
            );
            throw new ValidationException(
                    "Expense category is required",
                    "ERR_EXPENSE_CATEGORY_REQUIRED"
            );
        }

        String attachmentUrl =
                normalizeOptionalText(request.getAttachmentUrl());

        if (request.getExpenseCategory() == ExpenseCategory.GOVERNMENT_FEE
                && attachmentUrl == null) {

            throw new ValidationException(
                    "Portal challan attachment is required for government fee expense",
                    "ERR_GOVERNMENT_FEE_ATTACHMENT_REQUIRED"
            );
        }

        BigDecimal requestedAmount = normalizePositiveAmount(
                request.getAmount(),
                "Expense amount must be greater than zero",
                "ERR_INVALID_EXPENSE_AMOUNT"
        );

        String remark = requireText(
                request.getRemark(),
                "Expense remark is required",
                "ERR_EXPENSE_REMARK_REQUIRED"
        );

        if (request.getExpenseDate() != null &&
                request.getExpenseDate().isAfter(LocalDateTime.now())) {

            log.warn(
                    "[EXPENSE-CREATE-VALIDATION-FAILED] projectId={} | userId={} | reason=future-expense-date",
                    projectId,
                    user.getId()
            );
            throw new ValidationException(
                    "Expense date cannot be in the future",
                    "ERR_FUTURE_EXPENSE_DATE"
            );
        }

        String currencyCode = normalizeCurrencyCode(
                request.getCurrencyCode()
        );

        String categoryName = request.getExpenseCategory()
                .name()
                .replace("_", " ");

        String activitySummary =
                categoryName
                        + " - "
                        + currencyCode
                        + " "
                        + requestedAmount
                        + " - Pending CRT approval";

        ProjectActivity activity = createActivity(
                project,
                ActivityType.EXPENSE,
                "Expense Request Raised",
                activitySummary,
                user,
                false
        );

        activity = activityRepository.save(activity);

        log.debug(
                "[EXPENSE-ACTIVITY-SAVED] projectId={} | activityId={} | userId={}",
                projectId,
                activity.getId(),
                user.getId()
        );

        ProjectExpense expense = new ProjectExpense();

        expense.setProject(project);
        expense.setActivity(activity);

        expense.setRaisedDepartmentId(department.getId());
        expense.setRaisedDepartmentName(department.getName());

        expense.setExpenseCategory(request.getExpenseCategory());
        expense.setRequestedAmount(requestedAmount);
        expense.setApprovedAmount(null);
        expense.setPaidAmount(BigDecimal.ZERO);

        expense.setCurrencyCode(currencyCode);
        expense.setRemark(remark);

        expense.setExpenseDate(
                request.getExpenseDate() != null
                        ? request.getExpenseDate()
                        : LocalDateTime.now()
        );

        expense.setAttachmentUrl(attachmentUrl);

        expense.setExternalReference(
                normalizeOptionalText(request.getExternalReference())
        );

        expense.setCreatedByUserId(user.getId());
        expense.setCreatedByUserName(user.getFullName());

        expense.setApprovalStatus(ApprovalStatus.PENDING);
        expense.setApprovalStage(ExpenseApprovalStage.CRT_REVIEW);

        expense.setCrtApprovalStatus(ApprovalStatus.PENDING);
        expense.setAccountsApprovalStatus(ApprovalStatus.PENDING);

        expense.setExpensePaidBy(null);

        expense.setPaymentStatus(
                ExpensePaymentStatus.NOT_INITIATED
        );

        expense.setAccountPostingStatus(
                AccountPostingStatus.NOT_REQUIRED
        );
        expense.setAccountVoucherId(null);
        expense.setAccountVoucherNumber(null);
        expense.setAccountPostedAt(null);
        expense.setAccountPostingError(null);

        expense = expenseRepository.save(expense);

        log.info(
                "[EXPENSE-CREATE-SUCCESS] projectId={} | expenseId={} | activityId={} | stage={} | approvalStatus={} | paymentStatus={}",
                projectId,
                expense.getId(),
                activity.getId(),
                expense.getApprovalStage(),
                expense.getApprovalStatus(),
                expense.getPaymentStatus()
        );

        return mapResponse(
                activity,
                mapToExpenseDto(expense)
        );
    }

    private void validateGovernmentFeeRaiser(
            Department department,
            ExpenseCategory expenseCategory
    ) {

        if (expenseCategory != ExpenseCategory.GOVERNMENT_FEE) {
            return;
        }

        String departmentName =
                department.getName() == null
                        ? ""
                        : department.getName()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (!departmentName.contains("TECHNICAL")) {
            throw new ValidationException(
                    "Government fee expense can only be raised by the Technical department",
                    "ERR_GOVERNMENT_FEE_TECHNICAL_ONLY"
            );
        }
    }

    // =========================================================
    // CRT DECISION
    // =========================================================

    @Override
    @Transactional
    public ProjectExpenseResponseDto takeCrtExpenseDecision(
            Long projectId,
            Long expenseId,
            Long userId,
            CrtExpenseDecisionRequestDto request
    ) {

        log.info(
                "[CRT-DECISION-START] projectId={} | expenseId={} | userId={} | decision={} | paidBy={} | paymentMode={}",
                projectId,
                expenseId,
                userId,
                request != null ? request.getStatus() : null,
                request != null ? request.getExpensePaidBy() : null,
                request != null ? request.getClientPaymentMode() : null
        );

        if (request == null) {
            throw new ValidationException(
                    "CRT decision request is required",
                    "ERR_CRT_DECISION_REQUIRED"
            );
        }

        User user = validateActiveUser(userId);
        validateCrtApprover(user);

        Project project = validateActiveProject(projectId);

        ProjectExpense expense = validateExpense(
                project,
                expenseId
        );

        ApprovalStatus decision =
                validateDecisionStatus(request.getStatus());

        if (expense.getApprovalStage()
                != ExpenseApprovalStage.CRT_REVIEW) {

            throw new ValidationException(
                    "Expense is not pending at CRT review stage",
                    "ERR_INVALID_APPROVAL_STAGE"
            );
        }

        if (expense.getApprovalStatus() == ApprovalStatus.APPROVED
                || expense.getApprovalStatus() == ApprovalStatus.REJECTED
                || expense.getApprovalStatus() == ApprovalStatus.CANCELLED) {

            throw new ValidationException(
                    "Expense approval workflow is already completed",
                    "ERR_EXPENSE_ALREADY_COMPLETED"
            );
        }

        String decisionRemark =
                normalizeOptionalText(request.getRemark());

        validateDecisionRemark(
                decision,
                decisionRemark
        );

        LocalDateTime actionTime = LocalDateTime.now();

        expense.setCrtApprovalStatus(decision);
        expense.setCrtActionByUserId(user.getId());
        expense.setCrtActionByUserName(user.getFullName());
        expense.setCrtActionDate(actionTime);
        expense.setCrtDecisionRemark(decisionRemark);

        switch (decision) {

            case APPROVED -> processCrtApproval(
                    expense,
                    request,
                    actionTime
            );

            case REJECTED -> processCrtRejection(expense);

            case ON_HOLD -> processCrtOnHold(expense);

            default -> throw new ValidationException(
                    "Invalid CRT decision",
                    "ERR_INVALID_CRT_DECISION"
            );
        }

        expense = expenseRepository.save(expense);

        createExpenseDecisionActivity(
                project,
                expense,
                user,
                "CRT",
                decision
        );

        log.info(
                "[CRT-DECISION-SUCCESS] projectId={} | expenseId={} | userId={} | decision={} | paidBy={} | stage={} | paymentStatus={} | accountingCalled=false",
                projectId,
                expense.getId(),
                user.getId(),
                decision,
                expense.getExpensePaidBy(),
                expense.getApprovalStage(),
                expense.getPaymentStatus()
        );

        return mapToExpenseDto(expense);
    }

    private void processCrtApproval(
            ProjectExpense expense,
            CrtExpenseDecisionRequestDto request,
            LocalDateTime actionTime
    ) {

        ExpensePaidBy paidBy = request.getExpensePaidBy();

        if (paidBy == null) {
            throw new ValidationException(
                    "Expense paid by is required when CRT approves the expense",
                    "ERR_EXPENSE_PAID_BY_REQUIRED"
            );
        }

        expense.setExpensePaidBy(paidBy);

        if (paidBy == ExpensePaidBy.CLIENT_TO_COMPANY) {

            validateClientToCompanyDetails(request);

            String paymentMode = normalizePaymentMode(
                    request.getClientPaymentMode()
            );

            expense.setClientPaymentMode(
                    paymentMode
            );

            if ("CASH".equals(paymentMode)) {
                expense.setClientPaymentBankLedgerId(null);
                expense.setClientPaymentBankName(null);
            } else {
                expense.setClientPaymentBankLedgerId(
                        request.getClientPaymentBankLedgerId()
                );

                expense.setClientPaymentBankName(
                        normalizeOptionalText(
                                request.getClientPaymentBankName()
                        )
                );
            }

            expense.setClientPaymentDate(
                    request.getClientPaymentDate()
            );

            expense.setClientPaymentReference(
                    requireText(
                            request.getClientPaymentReference(),
                            "Client payment reference is required",
                            "ERR_CLIENT_PAYMENT_REFERENCE_REQUIRED"
                    )
            );

            expense.setClientPaymentProofUrl(
                    requireText(
                            request.getClientPaymentProofUrl(),
                            "Client payment proof is required",
                            "ERR_CLIENT_PAYMENT_PROOF_REQUIRED"
                    )
            );

            moveExpenseToAccountsReview(expense);
            return;
        }

        if (paidBy == ExpensePaidBy.COMPANY) {

            clearClientFundingDetails(expense);
            moveExpenseToAccountsReview(expense);
            return;
        }

        if (isClientDirect(paidBy)) {

            clearClientFundingDetails(expense);

            expense.setApprovalStatus(
                    ApprovalStatus.APPROVED
            );

            expense.setApprovalStage(
                    ExpenseApprovalStage.COMPLETED
            );

            expense.setAccountsApprovalStatus(
                    ApprovalStatus.CANCELLED
            );

            expense.setApprovedAmount(
                    expense.getRequestedAmount()
            );

            expense.setPaidAmount(
                    expense.getRequestedAmount()
            );

            expense.setPaymentStatus(
                    ExpensePaymentStatus.CLIENT_PAID
            );

            expense.setPaymentCompletedDate(actionTime);

            expense.setAccountPostingStatus(
                    AccountPostingStatus.SKIPPED
            );

            clearAccountVoucherDetails(expense);
            return;
        }

        throw new ValidationException(
                "Unsupported expense paid by value",
                "ERR_UNSUPPORTED_EXPENSE_PAID_BY"
        );
    }

    private void moveExpenseToAccountsReview(
            ProjectExpense expense
    ) {

        expense.setApprovalStatus(
                ApprovalStatus.PENDING
        );

        expense.setApprovalStage(
                ExpenseApprovalStage.ACCOUNTS_REVIEW
        );

        expense.setAccountsApprovalStatus(
                ApprovalStatus.PENDING
        );

        expense.setApprovedAmount(null);
        expense.setPaidAmount(BigDecimal.ZERO);

        expense.setPaymentStatus(
                ExpensePaymentStatus.NOT_INITIATED
        );

        expense.setPaymentCompletedDate(null);

        /*
         * CRT only declared the transaction.
         * Accounts has not verified or posted it.
         */
        expense.setAccountPostingStatus(
                AccountPostingStatus.NOT_REQUIRED
        );

        clearAccountVoucherDetails(expense);
    }

    private void validateClientToCompanyDetails(
            CrtExpenseDecisionRequestDto request
    ) {

        String paymentMode =
                normalizePaymentMode(
                        request.getClientPaymentMode()
                );

        if (paymentMode == null) {
            throw new ValidationException(
                    "Client payment mode is required",
                    "ERR_CLIENT_PAYMENT_MODE_REQUIRED"
            );
        }

        if (BANK_DETAILS_REQUIRED_PAYMENT_MODES.contains(paymentMode)) {

            if (request.getClientPaymentBankLedgerId() == null
                    || request.getClientPaymentBankLedgerId() <= 0) {

                throw new ValidationException(
                        "Receiving company bank is required for payment mode "
                                + paymentMode,
                        "ERR_CLIENT_PAYMENT_BANK_REQUIRED"
                );
            }

            requireText(
                    request.getClientPaymentBankName(),
                    "Client payment bank name is required",
                    "ERR_CLIENT_PAYMENT_BANK_NAME_REQUIRED"
            );
        }

        if (request.getClientPaymentDate() == null) {
            throw new ValidationException(
                    "Client payment date is required",
                    "ERR_CLIENT_PAYMENT_DATE_REQUIRED"
            );
        }

        if (request.getClientPaymentDate().isAfter(LocalDate.now())) {
            throw new ValidationException(
                    "Client payment date cannot be in the future",
                    "ERR_FUTURE_CLIENT_PAYMENT_DATE"
            );
        }

        requireText(
                request.getClientPaymentReference(),
                "Client payment reference is required",
                "ERR_CLIENT_PAYMENT_REFERENCE_REQUIRED"
        );

        requireText(
                request.getClientPaymentProofUrl(),
                "Client payment proof is required",
                "ERR_CLIENT_PAYMENT_PROOF_REQUIRED"
        );
    }

    private void processCrtRejection(
            ProjectExpense expense
    ) {

        expense.setExpensePaidBy(null);
        clearClientFundingDetails(expense);

        expense.setApprovalStatus(
                ApprovalStatus.REJECTED
        );

        expense.setApprovalStage(
                ExpenseApprovalStage.COMPLETED
        );

        expense.setAccountsApprovalStatus(
                ApprovalStatus.CANCELLED
        );

        expense.setApprovedAmount(null);
        expense.setPaidAmount(BigDecimal.ZERO);

        expense.setPaymentStatus(
                ExpensePaymentStatus.CANCELLED
        );

        expense.setPaymentCompletedDate(null);

        expense.setAccountPostingStatus(
                AccountPostingStatus.NOT_REQUIRED
        );

        clearAccountVoucherDetails(expense);
    }

    private void processCrtOnHold(
            ProjectExpense expense
    ) {

        expense.setApprovalStatus(
                ApprovalStatus.ON_HOLD
        );

        expense.setApprovalStage(
                ExpenseApprovalStage.CRT_REVIEW
        );

        expense.setAccountsApprovalStatus(
                ApprovalStatus.PENDING
        );

        expense.setApprovedAmount(null);
        expense.setPaidAmount(BigDecimal.ZERO);

        expense.setPaymentStatus(
                ExpensePaymentStatus.NOT_INITIATED
        );

        expense.setPaymentCompletedDate(null);

        expense.setAccountPostingStatus(
                AccountPostingStatus.NOT_REQUIRED
        );

        clearAccountVoucherDetails(expense);
    }

    private String normalizePaymentMode(
            String value
    ) {

        String paymentMode = normalizeOptionalText(value);

        if (paymentMode == null) {
            return null;
        }

        paymentMode = paymentMode
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");

        if (!ALLOWED_CLIENT_PAYMENT_MODES.contains(paymentMode)) {
            throw new ValidationException(
                    "Unsupported client payment mode: " + paymentMode,
                    "ERR_INVALID_CLIENT_PAYMENT_MODE"
            );
        }

        return paymentMode;
    }

    private void clearClientFundingDetails(
            ProjectExpense expense
    ) {

        expense.setClientPaymentMode(null);
        expense.setClientPaymentBankLedgerId(null);
        expense.setClientPaymentBankName(null);
        expense.setClientPaymentDate(null);
        expense.setClientPaymentReference(null);
        expense.setClientPaymentProofUrl(null);
    }

    private void clearAccountVoucherDetails(
            ProjectExpense expense
    ) {

        expense.setAccountVoucherId(null);
        expense.setAccountVoucherNumber(null);
        expense.setAccountPostedAt(null);
        expense.setAccountPostingError(null);
        expense.setReceiptVoucherId(null);
        expense.setReceiptVoucherNumber(null);
        expense.setInitialJournalVoucherId(null);
        expense.setInitialJournalVoucherNumber(null);
    }

    private boolean isClientDirect(
            ExpensePaidBy paidBy
    ) {
        return paidBy == ExpensePaidBy.CLIENT_DIRECT
                || paidBy == ExpensePaidBy.CLIENT;
    }

    // =========================================================
    // ACCOUNTS DECISION
    // =========================================================

    @Override
    @Transactional
    public ProjectExpenseResponseDto takeAccountsExpenseDecision(
            Long projectId,
            Long expenseId,
            Long userId,
            AccountsExpenseDecisionRequestDto request
    ) {

        log.info(
                "[ACCOUNTS-DECISION-START] projectId={} | expenseId={} | userId={} | requestedDecision={}",
                projectId,
                expenseId,
                userId,
                request != null ? request.getStatus() : null
        );

        User user = validateActiveUser(userId);
        validateAccountsApprover(user);

        Project project = validateProject(projectId);
        ProjectExpense expense = validateExpense(
                project,
                expenseId
        );

        log.debug(
                "[ACCOUNTS-EXPENSE-LOADED] expenseId={} | requestedAmount={} | currentStage={} | approvalStatus={} | crtStatus={} | accountsStatus={}",
                expense.getId(),
                expense.getRequestedAmount(),
                expense.getApprovalStage(),
                expense.getApprovalStatus(),
                expense.getCrtApprovalStatus(),
                expense.getAccountsApprovalStatus()
        );

        if (request == null) {
            log.warn(
                    "[ACCOUNTS-DECISION-VALIDATION-FAILED] projectId={} | expenseId={} | userId={} | reason=request-null",
                    projectId,
                    expenseId,
                    userId
            );
            throw new ValidationException(
                    "Accounts decision request is required",
                    "ERR_ACCOUNTS_DECISION_REQUIRED"
            );
        }

        if (request.getApprovalDate() != null
                && request.getApprovalDate().isAfter(LocalDate.now())) {

            throw new ValidationException(
                    "Approval date cannot be in the future",
                    "ERR_FUTURE_ACCOUNTS_APPROVAL_DATE"
            );
        }

        ApprovalStatus decision = validateDecisionStatus(
                request.getStatus()
        );

        if (expense.getApprovalStage() !=
                ExpenseApprovalStage.ACCOUNTS_REVIEW) {

            log.warn(
                    "[ACCOUNTS-DECISION-VALIDATION-FAILED] expenseId={} | currentStage={} | expectedStage={}",
                    expenseId,
                    expense.getApprovalStage(),
                    ExpenseApprovalStage.ACCOUNTS_REVIEW
            );
            throw new ValidationException(
                    "Expense is not pending at Accounts review stage",
                    "ERR_INVALID_APPROVAL_STAGE"
            );
        }

        if (expense.getCrtApprovalStatus() !=
                ApprovalStatus.APPROVED) {

            log.warn(
                    "[ACCOUNTS-DECISION-VALIDATION-FAILED] expenseId={} | crtStatus={} | reason=crt-not-approved",
                    expenseId,
                    expense.getCrtApprovalStatus()
            );
            throw new ValidationException(
                    "CRT approval is required before Accounts approval",
                    "ERR_CRT_APPROVAL_REQUIRED"
            );
        }

        if (expense.getApprovalStatus() == ApprovalStatus.REJECTED ||
                expense.getApprovalStatus() == ApprovalStatus.APPROVED) {

            log.warn(
                    "[ACCOUNTS-DECISION-VALIDATION-FAILED] expenseId={} | approvalStatus={} | reason=workflow-completed",
                    expenseId,
                    expense.getApprovalStatus()
            );
            throw new ValidationException(
                    "Expense approval workflow is already completed",
                    "ERR_EXPENSE_ALREADY_COMPLETED"
            );
        }

        String decisionRemark = normalizeOptionalText(
                request.getRemark()
        );

        validateDecisionRemark(
                decision,
                decisionRemark
        );

        LocalDateTime actionTime =
                request.getApprovalDate() != null
                        ? request.getApprovalDate().atTime(LocalTime.now())
                        : LocalDateTime.now();

        log.info(
                "[ACCOUNTS-DECISION-PROCESSING] expenseId={} | decision={} | userId={}",
                expenseId,
                decision,
                user.getId()
        );

        expense.setAccountsApprovalStatus(decision);
        expense.setAccountsActionByUserId(user.getId());
        expense.setAccountsActionByUserName(user.getFullName());
        expense.setAccountsActionDate(actionTime);
        expense.setAccountsDecisionRemark(decisionRemark);

        switch (decision) {

            case APPROVED -> approveExpenseByAccounts(
                    expense,
                    request,
                    decisionRemark
            );

            case REJECTED -> {
                expense.setApprovalStatus(ApprovalStatus.REJECTED);
                expense.setApprovalStage(
                        ExpenseApprovalStage.COMPLETED
                );

                expense.setApprovedAmount(null);
                expense.setPaidAmount(BigDecimal.ZERO);

                expense.setPaymentStatus(
                        ExpensePaymentStatus.CANCELLED
                );

                expense.setPaymentCompletedDate(null);
                expense.setAccountPostingStatus(
                        AccountPostingStatus.NOT_REQUIRED
                );
                expense.setAccountVoucherId(null);
                expense.setAccountVoucherNumber(null);
                expense.setAccountPostedAt(null);
                expense.setAccountPostingError(null);
            }

            case ON_HOLD -> {
                expense.setApprovalStatus(ApprovalStatus.ON_HOLD);
                expense.setApprovalStage(
                        ExpenseApprovalStage.ACCOUNTS_REVIEW
                );

                /*
                 * Preserve CLIENT_PAID because client-paid expenses must not
                 * become company-payable while Accounts has placed them on hold.
                 */
                if (expense.getExpensePaidBy() == ExpensePaidBy.COMPANY) {
                    expense.setPaymentStatus(
                            ExpensePaymentStatus.NOT_INITIATED
                    );
                }

                expense.setAccountPostingStatus(
                        AccountPostingStatus.NOT_REQUIRED
                );
            }

            default -> throw new ValidationException(
                    "Invalid Accounts decision",
                    "ERR_INVALID_ACCOUNTS_DECISION"
            );
        }

        expense = expenseRepository.save(expense);

        log.info(
                "[ACCOUNTS-EXPENSE-SAVED] expenseId={} | decision={} | approvedAmount={} | approvalStatus={} | paymentStatus={}",
                expense.getId(),
                decision,
                expense.getApprovedAmount(),
                expense.getApprovalStatus(),
                expense.getPaymentStatus()
        );

        createExpenseDecisionActivity(
                project,
                expense,
                user,
                "Accounts",
                decision
        );

        /*
         * Account Service books the approved Government Fee as:
         *
         * Government Fee Expense Dr
         *     Government Fee Payable Cr
         *
         * This is an approval-time payable posting. The actual company
         * payment remains PENDING and is handled separately.
         */
        if (decision == ApprovalStatus.APPROVED
                && requiresApprovalPosting(expense.getExpensePaidBy())
                && expense.getExpenseCategory() == ExpenseCategory.GOVERNMENT_FEE) {

            scheduleAccountPostingAfterCommit(
                    expense.getId()
            );
        }

        log.info(
                "[ACCOUNTS-DECISION-SUCCESS] projectId={} | expenseId={} | userId={} | decision={}",
                projectId,
                expense.getId(),
                user.getId(),
                decision
        );

        return mapToExpenseDto(expense);
    }


    private boolean requiresApprovalPosting(
            ExpensePaidBy paidBy
    ) {
        return paidBy == ExpensePaidBy.COMPANY
                || paidBy == ExpensePaidBy.CLIENT_TO_COMPANY;
    }


    private void approveExpenseByAccounts(
            ProjectExpense expense,
            AccountsExpenseDecisionRequestDto request,
            String decisionRemark
    ) {

        BigDecimal approvedAmount = normalizePositiveAmount(
                request.getApprovedAmount(),
                "Approved amount must be greater than zero",
                "ERR_INVALID_APPROVED_AMOUNT"
        );

        if (approvedAmount.compareTo(expense.getRequestedAmount()) > 0) {
            throw new ValidationException(
                    "Approved amount cannot exceed requested amount",
                    "ERR_APPROVED_AMOUNT_EXCEEDS_REQUESTED"
            );
        }

        if (approvedAmount.compareTo(expense.getRequestedAmount()) != 0
                && decisionRemark == null) {

            throw new ValidationException(
                    "Remark is required when approved amount differs from requested amount",
                    "ERR_PARTIAL_APPROVAL_REMARK_REQUIRED"
            );
        }

        ExpensePaidBy paidBy = expense.getExpensePaidBy();

        if (paidBy == null) {
            throw new ValidationException(
                    "CRT must decide who will fund the expense",
                    "ERR_EXPENSE_PAID_BY_NOT_DECIDED"
            );
        }

        expense.setApprovedAmount(approvedAmount);
        expense.setApprovalStatus(ApprovalStatus.APPROVED);
        expense.setApprovalStage(ExpenseApprovalStage.COMPLETED);

        /*
         * Client paid the government directly.
         * Nothing enters company books.
         */
        if (paidBy == ExpensePaidBy.CLIENT_DIRECT
                || paidBy == ExpensePaidBy.CLIENT) {

            expense.setPaidAmount(approvedAmount);
            expense.setPaymentStatus(
                    ExpensePaymentStatus.CLIENT_PAID
            );

            expense.setPaymentCompletedDate(
                    LocalDateTime.now()
            );

            expense.setAccountPostingStatus(
                    AccountPostingStatus.SKIPPED
            );

            clearAccountVoucherDetails(expense);
            return;
        }

        /*
         * COMPANY and CLIENT_TO_COMPANY both require
         * approval-time accounting.
         *
         * Actual government payment has not happened yet.
         */
        if (paidBy == ExpensePaidBy.COMPANY
                || paidBy == ExpensePaidBy.CLIENT_TO_COMPANY) {

            expense.setPaidAmount(BigDecimal.ZERO);

            expense.setPaymentStatus(
                    ExpensePaymentStatus.PENDING
            );

            expense.setPaymentCompletedDate(null);

            expense.setAccountPostingStatus(
                    AccountPostingStatus.PENDING
            );

            clearAccountVoucherDetails(expense);
            return;
        }

        throw new ValidationException(
                "Unsupported expense paid by value: " + paidBy,
                "ERR_UNSUPPORTED_EXPENSE_PAID_BY"
        );
    }

    /**
     * Calls ExpenseAccountPostingServiceImpl through its interface after the
     * Accounts approval transaction commits. The implementation uses
     * REQUIRES_NEW and can therefore read the committed approved expense.
     */
    private void scheduleAccountPostingAfterCommit(
            Long expenseId
    ) {

        Runnable postingTask = () -> {
            try {
                log.info(
                        "[EXPENSE-ACCOUNT-POSTING-CALL-START] expenseId={}",
                        expenseId
                );

                /*
                 * ACTUAL METHOD CALL:
                 * Spring injects ExpenseAccountPostingServiceImpl because it
                 * implements ExpenseAccountPostingService and is a @Service.
                 */
                expenseAccountPostingService
                        .postGovernmentFeeExpense(expenseId);

                log.info(
                        "[EXPENSE-ACCOUNT-POSTING-CALL-SUCCESS] expenseId={}",
                        expenseId
                );

            } catch (Exception exception) {
                log.error(
                        "[EXPENSE-ACCOUNT-POSTING-CALL-FAILED] expenseId={}",
                        expenseId,
                        exception
                );
            }
        };

        if (TransactionSynchronizationManager
                .isSynchronizationActive()) {

            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    postingTask.run();
                                }
                            }
                    );

            log.info(
                    "[EXPENSE-ACCOUNT-POSTING-SCHEDULED-AFTER-COMMIT] expenseId={}",
                    expenseId
            );

        } else {
            /*
             * Fallback for execution without a Spring-managed transaction.
             */
            postingTask.run();
        }
    }

    // =========================================================
    // STEP 4 - GOVERNMENT-FEE FUND TRANSFER
    // =========================================================

    @Override
    public ProjectExpenseResponseDto transferGovernmentFeeFunds(
            Long projectId,
            Long expenseId,
            Long userId,
            GovernmentFeeFundTransferRequestDto request
    ) {

        log.info(
                "[GOVERNMENT-FEE-FUND-TRANSFER-START] projectId={} | expenseId={} | userId={} | fromBankLedgerId={} | toBankLedgerId={} | amount={}",
                projectId,
                expenseId,
                userId,
                request != null ? request.getFromBankLedgerId() : null,
                request != null ? request.getToBankLedgerId() : null,
                request != null ? request.getAmount() : null
        );

        if (request == null) {
            throw new ValidationException(
                    "Government-fee fund transfer request is required",
                    "ERR_FUND_TRANSFER_REQUEST_REQUIRED"
            );
        }

        Project project = validateActiveProject(projectId);

        /*
         * Verifies that expenseId exists and belongs to projectId.
         * Detailed approval/payment/bank validation is performed by
         * ExpenseAccountPostingServiceImpl.
         */
        validateExpense(project, expenseId);

        ProjectExpenseResponseDto response =
                expenseAccountPostingService.transferGovernmentFeeFunds(
                        expenseId,
                        userId,
                        request
                );

        log.info(
                "[GOVERNMENT-FEE-FUND-TRANSFER-END] projectId={} | expenseId={} | paymentStatus={} | fundTransferPostingStatus={} | contraVoucherId={}",
                projectId,
                expenseId,
                response.getPaymentStatus(),
                response.getFundTransferPostingStatus(),
                response.getFundTransferVoucherId()
        );

        return response;
    }

    // =========================================================
    // APPROVAL QUEUE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProjectExpenseResponseDto> getExpenseApprovalQueue(
            Long userId,
            ExpenseApprovalStage approvalStage,
            ApprovalStatus approvalStatus
    ) {

        log.info(
                "[EXPENSE-APPROVAL-QUEUE-START] userId={} | approvalStage={} | approvalStatus={}",
                userId,
                approvalStage,
                approvalStatus
        );

        User user = validateActiveUser(userId);

        if (approvalStage == null) {
            log.warn(
                    "[EXPENSE-APPROVAL-QUEUE-VALIDATION-FAILED] userId={} | reason=approval-stage-null",
                    userId
            );
            throw new ValidationException(
                    "Approval stage is required",
                    "ERR_APPROVAL_STAGE_REQUIRED"
            );
        }

        if (approvalStage == ExpenseApprovalStage.CRT_REVIEW) {
            validateCrtApprover(user);
        } else if (
                approvalStage ==
                        ExpenseApprovalStage.ACCOUNTS_REVIEW
        ) {
            validateAccountsApprover(user);
        }

        List<ProjectExpense> expenses;

        if (approvalStatus == null) {
            expenses = expenseRepository
                    .findByApprovalStageOrderByExpenseDateDesc(
                            approvalStage
                    );
        } else {
            expenses = expenseRepository
                    .findByApprovalStageAndApprovalStatusOrderByExpenseDateDesc(
                            approvalStage,
                            approvalStatus
                    );
        }

        log.info(
                "[EXPENSE-APPROVAL-QUEUE-SUCCESS] userId={} | approvalStage={} | approvalStatus={} | recordCount={}",
                userId,
                approvalStage,
                approvalStatus,
                expenses.size()
        );

        return expenses.stream()
                .map(this::mapToExpenseDto)
                .toList();
    }

    // =========================================================
    // PAYMENT QUEUE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProjectExpenseResponseDto> getExpensePaymentQueue(
            Long userId,
            ExpensePaymentStatus paymentStatus
    ) {

        log.info(
                "[EXPENSE-PAYMENT-QUEUE-START] userId={} | paymentStatus={}",
                userId,
                paymentStatus
        );

        User user = validateActiveUser(userId);
        validateAccountsApprover(user);

        if (paymentStatus == ExpensePaymentStatus.CLIENT_PAID) {
            throw new ValidationException(
                    "Client-paid expenses are not part of the company payment queue",
                    "ERR_CLIENT_PAID_NOT_PAYMENT_QUEUE"
            );
        }

        List<ProjectExpense> expenses;

        if (paymentStatus == null) {
            expenses = expenseRepository
                    .findByPaymentStatusInOrderByExpenseDateDesc(
                            ACTIVE_PAYMENT_STATUSES
                    );
        } else {
            expenses = expenseRepository
                    .findByPaymentStatusOrderByExpenseDateDesc(
                            paymentStatus
                    );
        }

        log.info(
                "[EXPENSE-PAYMENT-QUEUE-SUCCESS] userId={} | paymentStatus={} | recordCount={}",
                userId,
                paymentStatus,
                expenses.size()
        );

        return expenses.stream()
                .map(this::mapToExpenseDto)
                .toList();
    }

    // =========================================================
    // EXPENSE BY PROJECT
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ProjectExpenseResponseDto> getExpensesByProject(
            Long projectId,
            Long userId
    ) {

        log.info(
                "[PROJECT-EXPENSES-START] projectId={} | userId={}",
                projectId,
                userId
        );

        validateActiveUser(userId);
        validateProject(projectId);

        List<ProjectExpense> expenses = expenseRepository
                .findByProjectIdOrderByExpenseDateDesc(projectId);

        log.info(
                "[PROJECT-EXPENSES-SUCCESS] projectId={} | userId={} | recordCount={}",
                projectId,
                userId,
                expenses.size()
        );

        return expenses
                .stream()
                .map(this::mapToExpenseDto)
                .toList();
    }

    // =========================================================
    // EXPENSE BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ProjectExpenseResponseDto getExpenseById(
            Long expenseId,
            Long userId
    ) {

        log.info(
                "[EXPENSE-DETAIL-START] expenseId={} | userId={}",
                expenseId,
                userId
        );

        validateActiveUser(userId);

        ProjectExpense expense = expenseRepository
                .findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expense not found with id: " + expenseId,
                        "ERR_EXPENSE_NOT_FOUND"
                ));

        log.info(
                "[EXPENSE-DETAIL-SUCCESS] expenseId={} | userId={} | projectId={} | stage={} | approvalStatus={} | paymentStatus={}",
                expense.getId(),
                userId,
                expense.getProject() != null ? expense.getProject().getId() : null,
                expense.getApprovalStage(),
                expense.getApprovalStatus(),
                expense.getPaymentStatus()
        );

        return mapToExpenseDto(expense);
    }

    // =========================================================
    // ACTIVITY FETCH
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectActivityResponseDto> getAllActivities(
            Long projectId,
            Pageable pageable
    ) {

        log.info(
                "[ACTIVITY-LIST-START] projectId={} | requestedPage={} | requestedSize={}",
                projectId,
                pageable != null ? pageable.getPageNumber() : null,
                pageable != null ? pageable.getPageSize() : null
        );

        validateProject(projectId);

        Pageable normalizedPageable =
                normalizePageable(pageable);

        Page<ProjectActivity> page = activityRepository
                .findByProjectIdAndDeletedFalseOrderByActivityDateDesc(
                        projectId,
                        normalizedPageable
                );

        List<ProjectActivityResponseDto> content =
                page.getContent()
                        .stream()
                        .map(activity -> {

                            if (activity.getActivityType() ==
                                    ActivityType.COMMENT) {

                                ProjectComment comment =
                                        commentRepository
                                                .findByActivityId(
                                                        activity.getId()
                                                )
                                                .orElse(null);

                                if (comment != null &&
                                        comment.getParentCommentId() != null) {
                                    return null;
                                }
                            }

                            return mapTimeline(activity);
                        })
                        .filter(Objects::nonNull)
                        .toList();

        log.info(
                "[ACTIVITY-LIST-SUCCESS] projectId={} | page={} | size={} | returnedCount={} | totalElements={}",
                projectId,
                normalizedPageable.getPageNumber(),
                normalizedPageable.getPageSize(),
                content.size(),
                page.getTotalElements()
        );

        return new PageImpl<>(
                content,
                normalizedPageable,
                page.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectActivityResponseDto> getActivitiesByType(
            Long projectId,
            ActivityType type,
            Pageable pageable
    ) {

        log.info(
                "[ACTIVITY-BY-TYPE-START] projectId={} | activityType={} | requestedPage={} | requestedSize={}",
                projectId,
                type,
                pageable != null ? pageable.getPageNumber() : null,
                pageable != null ? pageable.getPageSize() : null
        );

        validateProject(projectId);

        if (type == null) {
            log.warn(
                    "[ACTIVITY-BY-TYPE-VALIDATION-FAILED] projectId={} | reason=activity-type-null",
                    projectId
            );
            throw new ValidationException(
                    "Activity type is required",
                    "ERR_ACTIVITY_TYPE_REQUIRED"
            );
        }

        Pageable normalizedPageable =
                normalizePageable(pageable);

        Page<ProjectActivityResponseDto> response;

        if (type == ActivityType.COMMENT) {
            response = activityRepository
                    .findParentCommentActivities(
                            projectId,
                            type,
                            normalizedPageable
                    )
                    .map(this::mapTimeline);
        } else {
            response = activityRepository
                    .findByProjectIdAndActivityTypeAndDeletedFalseOrderByActivityDateDesc(
                            projectId,
                            type,
                            normalizedPageable
                    )
                    .map(this::mapTimeline);
        }

        log.info(
                "[ACTIVITY-BY-TYPE-SUCCESS] projectId={} | activityType={} | page={} | returnedCount={} | totalElements={}",
                projectId,
                type,
                response.getNumber(),
                response.getNumberOfElements(),
                response.getTotalElements()
        );

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectActivityResponseDto> getActivitiesByDateRange(
            Long projectId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {

        log.info(
                "[ACTIVITY-DATE-RANGE-START] projectId={} | startDate={} | endDate={} | requestedPage={} | requestedSize={}",
                projectId,
                startDate,
                endDate,
                pageable != null ? pageable.getPageNumber() : null,
                pageable != null ? pageable.getPageSize() : null
        );

        validateProject(projectId);

        if (startDate == null || endDate == null) {
            log.warn(
                    "[ACTIVITY-DATE-RANGE-VALIDATION-FAILED] projectId={} | startDate={} | endDate={} | reason=date-null",
                    projectId,
                    startDate,
                    endDate
            );
            throw new ValidationException(
                    "Start date and end date are required",
                    "ERR_DATE_RANGE_REQUIRED"
            );
        }

        if (startDate.isAfter(endDate)) {
            log.warn(
                    "[ACTIVITY-DATE-RANGE-VALIDATION-FAILED] projectId={} | startDate={} | endDate={} | reason=start-after-end",
                    projectId,
                    startDate,
                    endDate
            );
            throw new ValidationException(
                    "Start date cannot be after end date",
                    "ERR_INVALID_DATE_RANGE"
            );
        }

        Pageable normalizedPageable =
                normalizePageable(pageable);

        Page<ProjectActivity> page = activityRepository
                .findByProjectIdAndActivityDateBetweenAndDeletedFalseOrderByActivityDateDesc(
                        projectId,
                        startDate.atStartOfDay(),
                        endDate.atTime(23, 59, 59),
                        normalizedPageable
                );

        List<ProjectActivityResponseDto> content =
                page.getContent()
                        .stream()
                        .map(activity -> {

                            if (activity.getActivityType() ==
                                    ActivityType.COMMENT) {

                                ProjectComment comment =
                                        commentRepository
                                                .findByActivityId(
                                                        activity.getId()
                                                )
                                                .orElse(null);

                                if (comment != null &&
                                        comment.getParentCommentId() != null) {
                                    return null;
                                }
                            }

                            return mapTimeline(activity);
                        })
                        .filter(Objects::nonNull)
                        .toList();

        log.info(
                "[ACTIVITY-DATE-RANGE-SUCCESS] projectId={} | startDate={} | endDate={} | returnedCount={} | totalElements={}",
                projectId,
                startDate,
                endDate,
                content.size(),
                page.getTotalElements()
        );

        return new PageImpl<>(
                content,
                normalizedPageable,
                page.getTotalElements()
        );
    }

    // =========================================================
    // VALIDATION HELPERS
    // =========================================================

    private User validateActiveUser(Long userId) {

        log.debug(
                "[USER-VALIDATION-START] userId={}",
                userId
        );

        if (userId == null) {
            log.warn("[USER-VALIDATION-FAILED] reason=user-id-null");
            throw new ValidationException(
                    "User ID is required",
                    "ERR_USER_ID_REQUIRED"
            );
        }

        User user = userRepository
                .findActiveUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active user not found with id: " + userId,
                        "ERR_USER_NOT_FOUND"
                ));

        log.debug(
                "[USER-VALIDATION-SUCCESS] userId={}",
                user.getId()
        );

        return user;
    }

    private Project validateProject(Long projectId) {

        log.debug(
                "[PROJECT-VALIDATION-START] projectId={}",
                projectId
        );

        if (projectId == null) {
            log.warn("[PROJECT-VALIDATION-FAILED] reason=project-id-null");
            throw new ValidationException(
                    "Project ID is required",
                    "ERR_PROJECT_ID_REQUIRED"
            );
        }

        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId,
                        "ERR_PROJECT_NOT_FOUND"
                ));

        log.debug(
                "[PROJECT-VALIDATION-SUCCESS] projectId={} | active={} | deleted={} | cancelled={}",
                project.getId(),
                project.isActive(),
                project.isDeleted(),
                project.isCancelled()
        );

        return project;
    }

    private Project validateActiveProject(Long projectId) {

        Project project = validateProject(projectId);

        if (project.isDeleted()) {
            log.warn(
                    "[ACTIVE-PROJECT-VALIDATION-FAILED] projectId={} | reason=deleted",
                    projectId
            );
            throw new ValidationException(
                    "Expense cannot be raised for a deleted project",
                    "ERR_PROJECT_DELETED"
            );
        }

        if (!project.isActive()) {
            log.warn(
                    "[ACTIVE-PROJECT-VALIDATION-FAILED] projectId={} | reason=inactive",
                    projectId
            );
            throw new ValidationException(
                    "Expense cannot be raised for an inactive project",
                    "ERR_PROJECT_INACTIVE"
            );
        }

        if (project.isCancelled()) {
            log.warn(
                    "[ACTIVE-PROJECT-VALIDATION-FAILED] projectId={} | reason=cancelled",
                    projectId
            );
            throw new ValidationException(
                    "Expense cannot be raised for a cancelled project",
                    "ERR_PROJECT_CANCELLED"
            );
        }

        log.debug(
                "[ACTIVE-PROJECT-VALIDATION-SUCCESS] projectId={}",
                projectId
        );

        return project;
    }

    private ProjectExpense validateExpense(
            Project project,
            Long expenseId
    ) {

        log.debug(
                "[EXPENSE-VALIDATION-START] projectId={} | expenseId={}",
                project != null ? project.getId() : null,
                expenseId
        );

        if (expenseId == null) {
            log.warn(
                    "[EXPENSE-VALIDATION-FAILED] projectId={} | reason=expense-id-null",
                    project != null ? project.getId() : null
            );
            throw new ValidationException(
                    "Expense ID is required",
                    "ERR_EXPENSE_ID_REQUIRED"
            );
        }

        ProjectExpense expense = expenseRepository
                .findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Expense not found with id: " + expenseId,
                        "ERR_EXPENSE_NOT_FOUND"
                ));

        if (expense.getProject() == null ||
                !expense.getProject().getId().equals(project.getId())) {

            log.warn(
                    "[EXPENSE-VALIDATION-FAILED] selectedProjectId={} | expenseId={} | actualProjectId={} | reason=project-mismatch",
                    project.getId(),
                    expenseId,
                    expense.getProject() != null ? expense.getProject().getId() : null
            );
            throw new ValidationException(
                    "Expense does not belong to the selected project",
                    "ERR_EXPENSE_PROJECT_MISMATCH"
            );
        }

        log.debug(
                "[EXPENSE-VALIDATION-SUCCESS] projectId={} | expenseId={} | stage={} | approvalStatus={}",
                project.getId(),
                expense.getId(),
                expense.getApprovalStage(),
                expense.getApprovalStatus()
        );

        return expense;
    }

    private Department validateUserDepartment(
            User user,
            Long departmentId
    ) {

        log.debug(
                "[DEPARTMENT-VALIDATION-START] userId={} | departmentId={}",
                user != null ? user.getId() : null,
                departmentId
        );

        if (departmentId == null) {
            log.warn(
                    "[DEPARTMENT-VALIDATION-FAILED] userId={} | reason=department-id-null",
                    user != null ? user.getId() : null
            );
            throw new ValidationException(
                    "Department ID is required",
                    "ERR_DEPARTMENT_REQUIRED"
            );
        }

        if (user.getDepartments() == null) {
            log.warn(
                    "[DEPARTMENT-VALIDATION-FAILED] userId={} | departmentId={} | reason=no-user-departments",
                    user.getId(),
                    departmentId
            );
            throw new ValidationException(
                    "User is not assigned to any department",
                    "ERR_USER_DEPARTMENT_NOT_FOUND"
            );
        }

        Department department = user.getDepartments()
                .stream()
                .filter(Objects::nonNull)
                .filter(userDepartment ->
                        Objects.equals(
                                userDepartment.getId(),
                                departmentId
                        )
                )
                .filter(userDepartment -> !userDepartment.isDeleted())
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "User does not belong to the selected department",
                        "ERR_USER_DEPARTMENT_MISMATCH"
                ));

        log.debug(
                "[DEPARTMENT-VALIDATION-SUCCESS] userId={} | departmentId={}",
                user.getId(),
                department.getId()
        );

        return department;
    }

    private ApprovalStatus validateDecisionStatus(
            ApprovalStatus status
    ) {

        log.debug(
                "[DECISION-STATUS-VALIDATION-START] status={}",
                status
        );

        if (status == null) {
            log.warn("[DECISION-STATUS-VALIDATION-FAILED] reason=status-null");
            throw new ValidationException(
                    "Decision status is required",
                    "ERR_DECISION_STATUS_REQUIRED"
            );
        }

        if (!ALLOWED_DECISION_STATUSES.contains(status)) {
            log.warn(
                    "[DECISION-STATUS-VALIDATION-FAILED] status={} | allowedStatuses={}",
                    status,
                    ALLOWED_DECISION_STATUSES
            );
            throw new ValidationException(
                    "Allowed decision statuses are APPROVED, REJECTED and ON_HOLD",
                    "ERR_INVALID_DECISION_STATUS"
            );
        }

        log.debug(
                "[DECISION-STATUS-VALIDATION-SUCCESS] status={}",
                status
        );

        return status;
    }

    private void validateDecisionRemark(
            ApprovalStatus status,
            String remark
    ) {

        if ((status == ApprovalStatus.REJECTED ||
                status == ApprovalStatus.ON_HOLD) &&
                remark == null) {

            log.warn(
                    "[DECISION-REMARK-VALIDATION-FAILED] status={} | reason=remark-required",
                    status
            );
            throw new ValidationException(
                    "Decision remark is required for REJECTED or ON_HOLD status",
                    "ERR_DECISION_REMARK_REQUIRED"
            );
        }
    }

    // =========================================================
    // APPROVER AUTHORIZATION
    // =========================================================

    private void validateCrtApprover(User user) {

        if (isAdministrator(user) ||
                hasRoleContaining(user, "CRT") ||
                hasDepartmentContaining(user, "CRT") ||
                hasDepartmentContaining(
                        user,
                        "CUSTOMER RELATIONSHIP"
                )) {
            log.debug(
                    "[CRT-AUTHORIZATION-SUCCESS] userId={}",
                    user.getId()
            );
            return;
        }

        log.warn(
                "[CRT-AUTHORIZATION-FAILED] userId={}",
                user.getId()
        );
        throw new ValidationException(
                "User is not authorized to take a CRT expense decision",
                "ERR_CRT_APPROVAL_UNAUTHORIZED"
        );
    }

    private void validateAccountsApprover(User user) {

        if (isAdministrator(user) ||
                hasRoleContaining(user, "ACCOUNT") ||
                hasRoleContaining(user, "FINANCE") ||
                hasDepartmentContaining(user, "ACCOUNT") ||
                hasDepartmentContaining(user, "FINANCE")) {
            log.debug(
                    "[ACCOUNTS-AUTHORIZATION-SUCCESS] userId={}",
                    user.getId()
            );
            return;
        }

        log.warn(
                "[ACCOUNTS-AUTHORIZATION-FAILED] userId={}",
                user.getId()
        );
        throw new ValidationException(
                "User is not authorized to take an Accounts expense decision",
                "ERR_ACCOUNTS_APPROVAL_UNAUTHORIZED"
        );
    }

    private boolean isAdministrator(User user) {
        return hasRoleContaining(user, "ADMIN");
    }

    private boolean hasRoleContaining(
            User user,
            String expectedValue
    ) {

        if (user.getRoles() == null) {
            return false;
        }

        String normalizedExpected =
                normalizeName(expectedValue);

        return user.getRoles()
                .stream()
                .filter(Objects::nonNull)
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(this::normalizeName)
                .anyMatch(roleName ->
                        roleName.contains(normalizedExpected)
                );
    }

    private boolean hasDepartmentContaining(
            User user,
            String expectedValue
    ) {

        if (user.getDepartments() == null) {
            return false;
        }

        String normalizedExpected =
                normalizeName(expectedValue);

        return user.getDepartments()
                .stream()
                .filter(Objects::nonNull)
                .filter(department -> !department.isDeleted())
                .map(Department::getName)
                .filter(Objects::nonNull)
                .map(this::normalizeName)
                .anyMatch(departmentName ->
                        departmentName.contains(normalizedExpected)
                );
    }

    private String normalizeName(String value) {
        return value
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace("_", " ")
                .replace("-", " ");
    }

    // =========================================================
    // ACTIVITY HELPERS
    // =========================================================

    private ProjectActivity createActivity(
            Project project,
            ActivityType type,
            String title,
            String summary,
            User user,
            boolean systemGenerated
    ) {

        log.debug(
                "[ACTIVITY-BUILD-START] projectId={} | type={} | userId={} | systemGenerated={}",
                project.getId(),
                type,
                user.getId(),
                systemGenerated
        );

        LocalDateTime now = LocalDateTime.now();

        ProjectActivity activity = new ProjectActivity();
        activity.setProject(project);
        activity.setActivityType(type);
        activity.setTitle(title);
        activity.setSummary(summary);
        activity.setActivityDate(now);
        activity.setCreatedByUserId(user.getId());
        activity.setCreatedByUserName(user.getFullName());
        activity.setSystemGenerated(systemGenerated);
        activity.setDeleted(false);
        activity.setCreatedDate(now);
        activity.setUpdatedDate(now);

        log.debug(
                "[ACTIVITY-BUILD-SUCCESS] projectId={} | type={} | userId={}",
                project.getId(),
                type,
                user.getId()
        );

        return activity;
    }

    private void createExpenseDecisionActivity(
            Project project,
            ProjectExpense expense,
            User user,
            String approvalLevel,
            ApprovalStatus status
    ) {

        log.debug(
                "[EXPENSE-DECISION-ACTIVITY-START] projectId={} | expenseId={} | approvalLevel={} | status={} | userId={}",
                project.getId(),
                expense.getId(),
                approvalLevel,
                status,
                user.getId()
        );

        String currencyCode =
                expense.getCurrencyCode() != null
                        ? expense.getCurrencyCode()
                        : "INR";

        BigDecimal displayAmount =
                expense.getApprovedAmount() != null
                        ? expense.getApprovedAmount()
                        : expense.getRequestedAmount();

        String title;
        String summary;

        switch (status) {

            case APPROVED -> {
                title = approvalLevel + " Expense Approved";
                summary = approvalLevel
                        + " approved expense of "
                        + currencyCode
                        + " "
                        + displayAmount
                        + " by "
                        + user.getFullName();
            }

            case REJECTED -> {
                title = approvalLevel + " Expense Rejected";
                summary = approvalLevel
                        + " rejected expense of "
                        + currencyCode
                        + " "
                        + displayAmount
                        + " by "
                        + user.getFullName();
            }

            case ON_HOLD -> {
                title = approvalLevel + " Expense On Hold";
                summary = approvalLevel
                        + " placed expense of "
                        + currencyCode
                        + " "
                        + displayAmount
                        + " on hold by "
                        + user.getFullName();
            }

            default -> throw new ValidationException(
                    "Unsupported activity status",
                    "ERR_UNSUPPORTED_ACTIVITY_STATUS"
            );
        }

        ProjectActivity activity = createActivity(
                project,
                ActivityType.EXPENSE,
                title,
                summary,
                user,
                true
        );

        activity = activityRepository.save(activity);

        log.info(
                "[EXPENSE-DECISION-ACTIVITY-SAVED] projectId={} | expenseId={} | activityId={} | approvalLevel={} | status={}",
                project.getId(),
                expense.getId(),
                activity.getId(),
                approvalLevel,
                status
        );
    }

    // =========================================================
    // RESPONSE MAPPING
    // =========================================================

    private ProjectActivityResponseDto mapResponse(
            ProjectActivity activity,
            Object details
    ) {

        ProjectActivityResponseDto dto =
                new ProjectActivityResponseDto();

        dto.setActivityId(activity.getId());
        dto.setActivityType(activity.getActivityType());
        dto.setTitle(activity.getTitle());
        dto.setSummary(activity.getSummary());
        dto.setActivityDate(activity.getActivityDate());
        dto.setCreatedByUserId(
                activity.getCreatedByUserId()
        );
        dto.setCreatedByUserName(
                activity.getCreatedByUserName()
        );
        dto.setDetails(details);

        return dto;
    }

    private ProjectActivityResponseDto mapTimeline(
            ProjectActivity activity
    ) {

        Object details = null;

        switch (activity.getActivityType()) {

            case NOTE -> details = noteRepository
                    .findByActivityId(activity.getId())
                    .orElse(null);

            case COMMENT -> {
                ProjectComment rootComment =
                        commentRepository
                                .findByActivityId(activity.getId())
                                .orElse(null);

                if (rootComment != null) {
                    List<ProjectComment> allComments =
                            commentRepository.findByProjectId(
                                    activity.getProject().getId()
                            );

                    details = buildCommentTree(
                            rootComment,
                            allComments
                    );
                }
            }

            case EXPENSE -> details = expenseRepository
                    .findByActivityId(activity.getId())
                    .map(this::mapToExpenseDto)
                    .orElse(null);

            default -> {
                // No additional details.
            }
        }

        return mapResponse(activity, details);
    }

    private ProjectCommentResponseDto buildCommentTree(
            ProjectComment root,
            List<ProjectComment> allComments
    ) {

        ProjectCommentResponseDto dto =
                new ProjectCommentResponseDto();

        dto.setId(root.getId());
        dto.setCommentText(root.getCommentText());
        dto.setParentCommentId(root.getParentCommentId());
        dto.setCreatedDate(root.getCreatedDate());
        dto.setCreatedByUserId(root.getCreatedByUserId());
        dto.setCreatedByUserName(
                root.getCreatedByUserName()
        );

        List<ProjectCommentResponseDto> children =
                allComments.stream()
                        .filter(comment ->
                                Objects.equals(
                                        root.getId(),
                                        comment.getParentCommentId()
                                )
                        )
                        .map(comment ->
                                buildCommentTree(
                                        comment,
                                        allComments
                                )
                        )
                        .toList();

        dto.setChildren(children);

        return dto;
    }

    private ProjectExpenseResponseDto mapToExpenseDto(
            ProjectExpense expense
    ) {

        ProjectExpenseResponseDto dto =
                new ProjectExpenseResponseDto();

        dto.setExpenseId(expense.getId());

        dto.setActivityId(
                expense.getActivity() != null
                        ? expense.getActivity().getId()
                        : null
        );

        dto.setRaisedDepartmentId(
                expense.getRaisedDepartmentId()
        );

        dto.setRaisedDepartmentName(
                expense.getRaisedDepartmentName()
        );

        dto.setExpenseCategory(
                expense.getExpenseCategory()
        );

        dto.setExpensePaidBy(
                expense.getExpensePaidBy()
        );

        dto.setRequestedAmount(
                expense.getRequestedAmount()
        );

        dto.setApprovedAmount(
                expense.getApprovedAmount()
        );

        dto.setPaidAmount(
                expense.getPaidAmount()
        );

        dto.setOutstandingAmount(
                expense.getOutstandingAmount()
        );

        dto.setCurrencyCode(
                expense.getCurrencyCode()
        );

        dto.setRemark(expense.getRemark());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setAttachmentUrl(expense.getAttachmentUrl());

        dto.setExternalReference(
                expense.getExternalReference()
        );

        dto.setApprovalStatus(
                expense.getApprovalStatus()
        );

        dto.setApprovalStage(
                expense.getApprovalStage()
        );

        dto.setCrtApprovalStatus(
                expense.getCrtApprovalStatus()
        );

        dto.setCrtActionByUserId(
                expense.getCrtActionByUserId()
        );

        dto.setCrtActionByUserName(
                expense.getCrtActionByUserName()
        );

        dto.setCrtActionDate(
                expense.getCrtActionDate()
        );

        dto.setCrtDecisionRemark(
                expense.getCrtDecisionRemark()
        );

        dto.setClientPaymentMode(
                expense.getClientPaymentMode()
        );

        dto.setClientPaymentBankLedgerId(
                expense.getClientPaymentBankLedgerId()
        );

        dto.setClientPaymentBankName(
                expense.getClientPaymentBankName()
        );

        dto.setClientPaymentDate(
                expense.getClientPaymentDate()
        );

        dto.setClientPaymentReference(
                expense.getClientPaymentReference()
        );

        dto.setClientPaymentProofUrl(
                expense.getClientPaymentProofUrl()
        );

        dto.setAccountsApprovalStatus(
                expense.getAccountsApprovalStatus()
        );

        dto.setAccountsActionByUserId(
                expense.getAccountsActionByUserId()
        );

        dto.setAccountsActionByUserName(
                expense.getAccountsActionByUserName()
        );

        dto.setAccountsActionDate(
                expense.getAccountsActionDate()
        );

        dto.setAccountsDecisionRemark(
                expense.getAccountsDecisionRemark()
        );

        dto.setExpensePaidBy(
                expense.getExpensePaidBy()
        );

        dto.setPaymentStatus(
                expense.getPaymentStatus()
        );

        dto.setPaymentCompletedDate(
                expense.getPaymentCompletedDate()
        );

        dto.setAccountPostingStatus(
                expense.getAccountPostingStatus()
        );

        dto.setAccountVoucherId(
                expense.getAccountVoucherId()
        );

        dto.setAccountVoucherNumber(
                expense.getAccountVoucherNumber()
        );

        dto.setAccountPostedAt(
                expense.getAccountPostedAt()
        );

        dto.setAccountPostingError(
                expense.getAccountPostingError()
        );

        // Step 3 voucher audit
        dto.setReceiptVoucherId(
                expense.getReceiptVoucherId()
        );

        dto.setReceiptVoucherNumber(
                expense.getReceiptVoucherNumber()
        );

        dto.setInitialJournalVoucherId(
                expense.getInitialJournalVoucherId()
        );

        dto.setInitialJournalVoucherNumber(
                expense.getInitialJournalVoucherNumber()
        );

        // Step 4 fund-transfer audit and CONTRA voucher
        dto.setFundTransferPostingStatus(
                expense.getFundTransferPostingStatus()
        );

        dto.setFundTransferFromBankLedgerId(
                expense.getFundTransferFromBankLedgerId()
        );

        dto.setFundTransferFromBankName(
                expense.getFundTransferFromBankName()
        );

        dto.setFundTransferToBankLedgerId(
                expense.getFundTransferToBankLedgerId()
        );

        dto.setFundTransferToBankName(
                expense.getFundTransferToBankName()
        );

        dto.setFundTransferAmount(
                expense.getFundTransferAmount()
        );

        dto.setFundTransferDate(
                expense.getFundTransferDate()
        );

        dto.setFundTransferReference(
                expense.getFundTransferReference()
        );

        dto.setFundTransferProofUrl(
                expense.getFundTransferProofUrl()
        );

        dto.setFundTransferVoucherId(
                expense.getFundTransferVoucherId()
        );

        dto.setFundTransferVoucherNumber(
                expense.getFundTransferVoucherNumber()
        );

        dto.setFundTransferPostingError(
                expense.getFundTransferPostingError()
        );

        // Destination bank retained for Step 5.
        dto.setPaymentBankLedgerId(
                expense.getPaymentBankLedgerId()
        );

        dto.setPaymentBankName(
                expense.getPaymentBankName()
        );

        dto.setCreatedByUserId(
                expense.getCreatedByUserId()
        );

        dto.setCreatedByUserName(
                expense.getCreatedByUserName()
        );

        dto.setCreatedDate(expense.getCreatedDate());
        dto.setUpdatedDate(expense.getUpdatedDate());

        Project project = expense.getProject();

        if (project != null) {
            dto.setProjectId(project.getId());
            dto.setProjectNo(project.getProjectNo());
            dto.setProjectName(project.getName());
            dto.setUnbilledNumber(
                    project.getUnbilledNumber()
            );

            if (project.getProduct() != null) {
                dto.setProductName(
                        project.getProduct().getProductName()
                );
            }
        }

        return dto;
    }

    // =========================================================
    // GENERAL HELPERS
    // =========================================================

    private BigDecimal normalizePositiveAmount(
            BigDecimal amount,
            String message,
            String errorCode
    ) {

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            log.warn(
                    "[AMOUNT-VALIDATION-FAILED] amount={} | errorCode={}",
                    amount,
                    errorCode
            );
            throw new ValidationException(
                    message,
                    errorCode
            );
        }

        return amount.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private String requireText(
            String value,
            String message,
            String errorCode
    ) {

        String normalized =
                normalizeOptionalText(value);

        if (normalized == null) {
            log.warn(
                    "[TEXT-VALIDATION-FAILED] errorCode={}",
                    errorCode
            );
            throw new ValidationException(
                    message,
                    errorCode
            );
        }

        return normalized;
    }

    private String normalizeOptionalText(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private String normalizeCurrencyCode(String value) {

        String currencyCode =
                value == null || value.trim().isEmpty()
                        ? "INR"
                        : value.trim().toUpperCase(Locale.ROOT);

        if (!currencyCode.matches("^[A-Z]{3}$")) {
            log.warn(
                    "[CURRENCY-VALIDATION-FAILED] currencyCode={}",
                    currencyCode
            );
            throw new ValidationException(
                    "Currency code must contain exactly three letters",
                    "ERR_INVALID_CURRENCY_CODE"
            );
        }

        return currencyCode;
    }

    private Pageable normalizePageable(Pageable pageable) {

        if (pageable == null) {
            log.debug(
                    "[PAGEABLE-NORMALIZED] source=null | page=0 | size=20"
            );
            return PageRequest.of(0, 20);
        }

        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.min(
                Math.max(pageable.getPageSize(), 1),
                100
        );

        Pageable normalized = PageRequest.of(
                page,
                size,
                pageable.getSort()
        );

        log.debug(
                "[PAGEABLE-NORMALIZED] requestedPage={} | requestedSize={} | normalizedPage={} | normalizedSize={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                normalized.getPageNumber(),
                normalized.getPageSize()
        );

        return normalized;
    }
}







