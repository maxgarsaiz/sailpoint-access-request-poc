package com.maxgarsaiz.sailpoint.domain.service;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequestAttempt;
import com.maxgarsaiz.sailpoint.domain.port.in.ManageAccessRequestAttemptStateUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestAttemptRepositoryPort;

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
    
    @Override
    @Transactional
    public void markAttemptAsCompleted(UUID attemptId) {
        log.info("✅ Marking attempt {} as COMPLETED", attemptId);
        
        AccessRequestAttempt attempt = attemptRepository.findByIdOrThrow(attemptId);
        
        // Update attempt status
        attempt.markAsCompleted();
        attemptRepository.update(attempt);
        
        log.info("✅ Attempt {} marked as COMPLETED. Job will naturally complete.", attemptId);
    }
    
    @Override
    @Transactional
    public void markAttemptAsFailed(UUID attemptId, String errorMessage) {
        log.error("❌ Marking attempt {} as FAILED: {}", attemptId, errorMessage);
        
        AccessRequestAttempt attempt = attemptRepository.findByIdOrThrow(attemptId);
        
        // Update attempt status
        attempt.markAsFailed(errorMessage);
        attemptRepository.update(attempt);
        
        log.error("❌ Attempt {} marked as FAILED. Job will naturally complete.", attemptId);
    }
    
    @Override
    @Transactional
    public void handleJobExhaustedRetries(UUID attemptId) {
        log.warn("⚠️ Job exhausted retries for attempt {}. Status remains IN_PROGRESS for manual retry.", 
            attemptId);
        
        // Keep status as IN_PROGRESS for manual retry
        // Job will naturally stop after exhausting retries
        
        log.info("⚠️ Attempt {} remains IN_PROGRESS for manual investigation and retry", attemptId);
    }
}
