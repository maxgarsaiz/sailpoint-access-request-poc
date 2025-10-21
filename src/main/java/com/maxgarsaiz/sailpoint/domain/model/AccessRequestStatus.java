package com.maxgarsaiz.sailpoint.domain.model;

import java.util.Arrays;
import java.util.List;

public enum AccessRequestStatus {
    PENDING,
    POOLING,
    COMPLETED,
    FAILED;

    public boolean canTransitionTo(AccessRequestStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == POOLING;
            case POOLING -> newStatus == COMPLETED || newStatus == FAILED || newStatus == PENDING;
            case FAILED -> newStatus == PENDING || newStatus == POOLING;
            case COMPLETED -> false;
        };
    }

    public static List<AccessRequestStatus> getRetryableStatuses() {
        return Arrays.asList(FAILED, POOLING);
    }

    public static List<AccessRequestStatus> getNonCompletedStatuses() {
        return Arrays.asList(PENDING, POOLING, FAILED);
    }
}