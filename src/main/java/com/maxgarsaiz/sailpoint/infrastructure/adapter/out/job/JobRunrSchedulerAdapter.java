package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.job;

import com.maxgarsaiz.sailpoint.domain.port.in.ExecuteAccessRequestPoolingUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.JobSchedulerPort;
import com.maxgarsaiz.sailpoint.infrastructure.config.PollingConfigurationProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobRunrSchedulerAdapter implements JobSchedulerPort {
    
    private final JobScheduler jobScheduler;
    private final ExecuteAccessRequestPoolingUseCase executePoolingUseCase;
    private final PollingConfigurationProperties pollingConfig;
    
    @Override
    public void schedulePoolingJob(UUID attemptId) {
        Duration initialDelay = pollingConfig.getInitialDelay();
        String jobId = attemptId.toString();
        
        log.info("📅 Scheduling recurring pooling job for attempt: {} " +
            "(initial delay: {}s)", 
            attemptId, 
            initialDelay.toSeconds());
        
        jobScheduler.scheduleRecurrently(
            jobId,
            initialDelay,
            () -> executePoolingUseCase.executePooling(attemptId)
        );
        
        log.info("✅ Recurring pooling job scheduled with ID: {}", jobId);
    }
    
    @Override
    public void deleteRecurringJob(UUID attemptId) {
        String jobId = attemptId.toString();
        
        log.info("🗑️ Deleting recurring pooling job: {}", jobId);
        
        try {
            BackgroundJob.deleteRecurringJob(jobId);
            log.info("✅ Recurring job deleted: {}", jobId);
        } catch (Exception e) {
            log.error("💥 Failed to delete recurring job: {}", jobId, e);
            throw e;
        }
    }
}
