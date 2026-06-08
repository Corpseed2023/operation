package com.doc.em;

public enum LegalStatus {
    INITIATED,        // Request raised by operation/user
    PENDING,          // Waiting for legal person action
    IN_REVIEW,        // Legal person started checking
    NEED_MORE_INFO,   // Legal needs more document/info
    APPROVED,         // Legal approved
    DISAPPROVED,      // Legal rejected/disapproved
    GUIDANCE_GIVEN,   // Legal gave advice/solution
    COMPLETED,        // Request closed/completed
    CANCELLED         // Request cancelled
}