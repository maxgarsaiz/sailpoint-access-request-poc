package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.job;

import com.maxgarsaiz.sailpoint.domain.port.in.ExecuteAccessRequestPoolingUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.JobSchedulerPort;
import com.maxgarsaiz.sailpoint.infrastructure.config.PollingConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobRunrSchedulerAdapter implements JobSchedulerPort {
    
    private final JobScheduler jobScheduler;
    private final ExecuteAccessRequestPoolingUseCase executePoolingUseCase;
    private final PollingConfigurationProperties pollingConfig;
    
    @Override
    public void schedulePoolingJob(UUID accessRequestId) {
        Duration initialDelay = pollingConfig.getInitialDelay();
        Instant scheduledAt = Instant.now().plus(initialDelay);
        
        log.info("Scheduling pooling job for access request: {} with initial delay of {}s (max retries: {}, interval: {}s)", 
            accessRequestId, 
            initialDelay.toSeconds(),
            pollingConfig.getMaxRetries(),
            pollingConfig.getRetryIntervalSeconds());
        
        jobScheduler.schedule(accessRequestId, scheduledAt, () -> executePoolingUseCase.executePooling(accessRequestId));
        
        log.info("Pooling job scheduled for access request: {} at {}", accessRequestId, scheduledAt);
    }
}