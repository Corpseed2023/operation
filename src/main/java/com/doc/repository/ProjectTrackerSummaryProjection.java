package com.doc.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ProjectTrackerSummaryProjection {

    Long getProjectId();

    String getProjectNumber();

    BigDecimal getProjectValue();

    Long getCompanyId();

    String getCompanyName();

    Long getProductId();

    String getServiceName();

    Long getStageId();

    String getStage();

    String getPriority();

    LocalDate getDueDate();
}
