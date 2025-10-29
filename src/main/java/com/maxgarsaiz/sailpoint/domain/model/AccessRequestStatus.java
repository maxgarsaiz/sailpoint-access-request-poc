package com.maxgarsaiz.sailpoint.domain.model;

import java.util.List;

public enum AccessRequestStatus {
    PENDING,
    PROCESSING_IN_PROGRESS,
    PROCESSING_COMPLETED,
    PROCESSING_REQUIRES_ATTENTION;

    public static List<AccessRequestStatus> getNonCompletedStatuses() {
        return List.of(PENDING, PROCESSING_IN_PROGRESS, PROCESSING_REQUIRES_ATTENTION);
    }
}
