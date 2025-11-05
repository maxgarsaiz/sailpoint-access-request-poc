package com.maxgarsaiz.sailpoint.domain.model;

import java.util.List;

public enum AccessRequestStatus {
    PROCESSING_IN_PROGRESS,
    PROCESSING_COMPLETED,
    PROCESSING_REQUIRES_ATTENTION;

    public static List<AccessRequestStatus> getNonCompletedStatuses() {
        return List.of(PROCESSING_IN_PROGRESS, PROCESSING_REQUIRES_ATTENTION);
    }
}
