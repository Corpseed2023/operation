package com.doc.repository.projection;

public interface VendorAssignmentCountProjection {
    Long getVendorId();

    String getVendorName();

    Long getTotalAssignmentCount();

    Long getActiveCount();

    Long getCompletedCount();

    Long getPendingCount();
}
