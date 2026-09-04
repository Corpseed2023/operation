package com.doc.dto.project.portal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProjectPortalApprovalQueueResponseDto {

    /**
     * Logged-in Admin, Operation Head, or Technical Manager ID.
     */
    private Long userId;

    /**
     * Requested approval status, such as PENDING, APPROVED, or REJECTED.
     */
    private String requestedStatus;

    /**
     * Total records available across all pages.
     */
    private long totalRequests;

    /**
     * Total number of pages.
     */
    private int totalPages;

    /**
     * Current page number using 1-based indexing.
     */
    private int currentPage;

    /**
     * Maximum number of records requested per page.
     */
    private int pageSize;

    /**
     * Indicates whether this is the first page.
     */
    private boolean first;

    /**
     * Indicates whether this is the last page.
     */
    private boolean last;

    /**
     * Indicates whether another page is available.
     */
    private boolean hasNext;

    /**
     * Indicates whether a previous page is available.
     */
    private boolean hasPrevious;

    /**
     * Portal approval requests available on the current page.
     */
    private List<ProjectPortalDetailResponseDto> requests =
            new ArrayList<>();
}