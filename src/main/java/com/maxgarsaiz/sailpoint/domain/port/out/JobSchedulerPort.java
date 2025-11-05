package com.maxgarsaiz.sailpoint.domain.port.out;

import java.util.UUID;

public interface JobSchedulerPort {
    
    /**
     * Schedules a pooling job for an access request attempt.
     * Each invocation creates a new job with a unique ID to avoid conflicts
     * when re-scheduling. The job will execute once and can be re-scheduled
     * multiple times until the access request reaches a final state.
     * 
     * @param attemptId the ID of the attempt to poll
     */
    void schedulePoolingJob(UUID attemptId);
}
