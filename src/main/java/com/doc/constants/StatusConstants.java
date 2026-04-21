package com.doc.constants;

import java.util.Set;

/**
 * SYSTEM-DEFINED STATUS IDs - **NEVER CHANGE THESE VALUES**
 * These IDs are hardcoded in DocumentationApplication.java DB seeder.
 * Changing them will break data integrity across the entire application.
 */
public final class StatusConstants {

    private StatusConstants() {} // Prevent instantiation

    // ====================== PROJECT STATUSES ======================
    public static final Long PROJECT_OPEN_ID = 1L;
    public static final Long PROJECT_IN_PROGRESS_ID = 2L;
    public static final Long PROJECT_COMPLETED_ID = 3L;
    public static final Long PROJECT_CANCELLED_ID = 4L;
    public static final Long PROJECT_REFUNDED_ID = 5L;

    // ====================== MILESTONE STATUSES ======================
    public static final Long MILESTONE_NEW_ID = 1L;
    public static final Long MILESTONE_IN_PROGRESS_ID = 2L;
    public static final Long MILESTONE_COMPLETED_ID = 3L;
    public static final Long MILESTONE_REWORK_ID = 4L;
    public static final Long MILESTONE_ON_HOLD_ID = 5L;
    public static final Long MILESTONE_QUEUED_ID = 6L;
    public static final Long MILESTONE_REJECTED_ID = 7L;

    // ====================== DOCUMENT STATUSES ======================
    public static final Long DOC_PENDING_ID = 1L;
    public static final Long DOC_UPLOADED_ID = 2L;
    public static final Long DOC_VERIFIED_ID = 3L;
    public static final Long DOC_REJECTED_ID = 4L;

    // ====================== PAYMENT TYPE IDS ======================
    public static final Long PAYMENT_FULL_ID = 1L;
    public static final Long PAYMENT_PARTIAL_ID = 2L;
    public static final Long PAYMENT_INSTALLMENT_ID = 3L;
    public static final Long PAYMENT_PO_ID = 4L;

    // ====================== SYSTEM STATUS SETS ======================
    public static final Set<Long> SYSTEM_PROJECT_STATUS_IDS = Set.of(
            PROJECT_OPEN_ID, PROJECT_IN_PROGRESS_ID, PROJECT_COMPLETED_ID,
            PROJECT_CANCELLED_ID, PROJECT_REFUNDED_ID
    );

    public static final Set<Long> SYSTEM_MILESTONE_STATUS_IDS = Set.of(
            MILESTONE_NEW_ID, MILESTONE_IN_PROGRESS_ID, MILESTONE_COMPLETED_ID,
            MILESTONE_REWORK_ID, MILESTONE_ON_HOLD_ID, MILESTONE_QUEUED_ID, MILESTONE_REJECTED_ID
    );

    public static final Set<Long> ACTIVE_MILESTONE_STATUS_IDS = Set.of(
            MILESTONE_NEW_ID, MILESTONE_IN_PROGRESS_ID
    );

    public static final Set<Long> SYSTEM_DOCUMENT_STATUS_IDS = Set.of(
            DOC_PENDING_ID, DOC_UPLOADED_ID, DOC_VERIFIED_ID, DOC_REJECTED_ID
    );

    public static final Set<Long> SYSTEM_PAYMENT_TYPE_IDS = Set.of(
            PAYMENT_FULL_ID, PAYMENT_PARTIAL_ID, PAYMENT_INSTALLMENT_ID, PAYMENT_PO_ID
    );
}