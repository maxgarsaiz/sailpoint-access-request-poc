package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.job;

import com.maxgarsaiz.sailpoint.domain.port.in.ExecuteAccessRequestPoolingUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.JobSchedulerPort;
import com.maxgarsaiz.sailpoint.infrastructure.config.PollingConfigurationProperties;
import lombok.RequiredArgsConstructor;

import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * JobRunr adapter for scheduling background jobs.
 * Uses attemptId as job ID for idempotency.
 * JobServerFilter ensures no conflicts by waiting for job completion before re-scheduling.
 */
@Component
@RequiredArgsConstructor
public class JobRunrSchedulerAdapter implements JobSchedulerPort {

    private final JobScheduler jobScheduler;
    private final ExecuteAccessRequestPoolingUseCase poolingUseCase;
    private final PollingConfigurationProperties pollingConfig;

    /**
     * Schedules a pooling job with delay from configuration.
     * Uses attemptId directly as job ID for true idempotency.
     * 
     * ApplyStateFilter re-schedules the same job using job.scheduleAt(),
     * so we only need to create the initial job here.
     */
    @Override
    public void schedulePoolingJob(UUID attemptId) {
        Instant scheduledAt = Instant.now().plus(pollingConfig.getPoolingInterval());
        
        jobScheduler.schedule(
                attemptId,
                scheduledAt,
                () -> poolingUseCase.executePooling(attemptId)
        );
    }
}

