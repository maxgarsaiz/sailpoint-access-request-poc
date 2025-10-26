package com.maxgarsaiz.sailpoint.domain.model;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the lifecycle status of an access request.
 * 
 * State machine transitions:
 * <pre>
 * PENDING ──→ POOLING ──→ COMPLETED
 *               ↓
 *             FAILED ──→ PENDING (manual retry)
 *               ↓
 *             POOLING (manual retry)
 * </pre>
 * 
 * States:
 * - PENDING: Initial state, awaiting background processing
 * - POOLING: Being processed by background job, checking Sailpoint status
 * - COMPLETED: Successfully completed (terminal state)
 * - FAILED: Failed after retries, can be retried manually
 */
public enum AccessRequestStatus {
    /** Initial state - request created and awaiting processing */
    PENDING,
    
    /** Active processing - background job is checking Sailpoint status */
    POOLING,
    
    /** Terminal success state - request completed successfully in Sailpoint */
    COMPLETED,
    
    /** Failed state - processing failed, manual retry possible */
    FAILED;

    /**
     * Validates if a transition from current status to new status is allowed.
     * 
     * @param newStatus The target status to transition to
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(AccessRequestStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == POOLING;
            case POOLING -> newStatus == COMPLETED || newStatus == FAILED || newStatus == PENDING;
            case FAILED -> newStatus == PENDING || newStatus == POOLING;
            case COMPLETED -> false; // Terminal state - no transitions allowed
        };
    }

    /**
     * Returns statuses that represent retryable states.
     * These requests can be manually retried.
     * 
     * @return List of retryable statuses
     */
    public static List<AccessRequestStatus> getRetryableStatuses() {
        return Arrays.asList(FAILED, POOLING);
    }

    /**
     * Returns statuses that represent non-completed requests.
     * These requests are either pending, processing, or failed.
     * 
     * @return List of non-completed statuses
     */
    public static List<AccessRequestStatus> getNonCompletedStatuses() {
        return Arrays.asList(PENDING, POOLING, FAILED);
    }
}
