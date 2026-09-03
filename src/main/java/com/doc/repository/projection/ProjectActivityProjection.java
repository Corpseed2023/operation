package com.doc.repository.projection;

import java.util.Date;

public interface ProjectActivityProjection {
    String getStatusName();
    String getProjectName();
    String getCompanyName();
    Date getUpdatedDate();
}