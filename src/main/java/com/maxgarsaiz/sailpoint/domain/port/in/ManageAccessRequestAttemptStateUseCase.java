package com.maxgarsaiz.sailpoint.domain.port.in;

import java.util.UUID;

/**
 * Use case for managing access request attempt state transitions.
 * Handles state changes and coordinates with job scheduling.
 */
public interface ManageAccessRequestAttemptStateUseCase {
    
    /**
     * Marks an attempt as COMPLETED.
     * Also deletes the recurring job.
     * 
     * @param attemptId the ID of the attempt to mark as completed
     */
    void markAttemptAsCompleted(UUID attemptId);
    
    /**
     * Marks an attempt as FAILED due to Sailpoint returning FAILED status.
     * Also deletes the recurring job.
     * 
     * @param attemptId the ID of the attempt to mark as failed
     * @param errorMessage the error message from Sailpoint
     */
    void markAttemptAsFailed(UUID attemptId, String errorMessage);
    
    /**
     * Handles when a job fails after exhausting all retries (onFailedAfterRetries).
     * Deletes the recurring job but keeps attempt status as IN_PROGRESS
     * to allow manual retry.
     * 
     * @param attemptId the ID of the attempt
     */
    void handleJobExhaustedRetries(UUID attemptId);
}