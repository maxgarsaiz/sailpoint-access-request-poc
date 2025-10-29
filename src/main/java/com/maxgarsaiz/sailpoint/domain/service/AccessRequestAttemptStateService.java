package com.maxgarsaiz.sailpoint.domain.service;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequestAttempt;
import com.maxgarsaiz.sailpoint.domain.port.in.ManageAccessRequestAttemptStateUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestAttemptRepositoryPort;
import com.maxgarsaiz.sailpoint.domain.port.out.JobSchedulerPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service that manages access request attempt state transitions.
 * Coordinates between domain entities and infrastructure (job scheduling).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRequestAttemptStateService implements ManageAccessRequestAttemptStateUseCase {
    
    private final AccessRequestAttemptRepositoryPort attemptRepository;
    private final JobSchedulerPort jobSchedulerPort;
    
    @Override
    @Transactional
    public void markAttemptAsCompleted(UUID attemptId) {
        log.info("✅ Marking attempt {} as COMPLETED", attemptId);
        
        AccessRequestAttempt attempt = attemptRepository.findByIdOrThrow(attemptId);
        
        // 1. Update attempt status
        attempt.markAsCompleted();
        attemptRepository.update(attempt);
        
        // 2. Delete recurring job
        jobSchedulerPort.deleteRecurringJob(attemptId);
        
        log.info("✅ Attempt {} marked as COMPLETED and job deleted", attemptId);
    }
    
    @Override
    @Transactional
    public void markAttemptAsFailed(UUID attemptId, String errorMessage) {
        log.error("❌ Marking attempt {} as FAILED: {}", attemptId, errorMessage);
        
        AccessRequestAttempt attempt = attemptRepository.findByIdOrThrow(attemptId);
        
        // 1. Update attempt status
        attempt.markAsFailed(errorMessage);
        attemptRepository.update(attempt);
        
        // 2. Delete recurring job
        jobSchedulerPort.deleteRecurringJob(attemptId);
        
        log.error("❌ Attempt {} marked as FAILED and job deleted", attemptId);
    }
    
    @Override
    @Transactional
    public void handleJobExhaustedRetries(UUID attemptId) {
        log.warn("⚠️ Job exhausted retries for attempt {}, deleting job but keeping status IN_PROGRESS", 
            attemptId);
        
        // Only delete the job, keep status as IN_PROGRESS for manual retry
        jobSchedulerPort.deleteRecurringJob(attemptId);
        
        log.info("🗑️ Recurring job deleted for attempt {} (status remains IN_PROGRESS)", attemptId);
    }
}
