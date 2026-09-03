package com.doc.repository.projection;

import java.time.LocalDateTime;

public interface MilestoneActivityProjection {
    String getMilestoneName();
    String getCompanyName();
    LocalDateTime getCompletedDate();
}