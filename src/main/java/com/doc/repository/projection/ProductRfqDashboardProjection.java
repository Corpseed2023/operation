package com.doc.repository.projection;

import java.util.Date;

public interface ProductRfqDashboardProjection {
    Long getRfqId();

    String getRfqNumber();

    String getTitle();

    Date getQuotationSubmissionDeadline();

    Long getVendorsInvited();

    Long getQuotationsReceived();

    String getStatus();
}
