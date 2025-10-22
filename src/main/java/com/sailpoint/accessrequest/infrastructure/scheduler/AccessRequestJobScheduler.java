package com.sailpoint.accessrequest.infrastructure.scheduler;

import com.sailpoint.accessrequest.domain.service.AccessRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessRequestJobScheduler {

    private final JobScheduler jobScheduler;
    private final AccessRequestService accessRequestService;

    @Job(name = "Process Access Request", retries = 3)
    public void processAccessRequest(Long requestId) {
        log.info("JobRunr: Processing access request {}", requestId);
        accessRequestService.processAccessRequest(requestId);
    }

    public void scheduleProcessing(Long requestId) {
        jobScheduler.enqueue(() -> processAccessRequest(requestId));
        log.info("Scheduled processing for access request {}", requestId);
    }

    public void scheduleDelayedProcessing(Long requestId, Duration delay) {
        jobScheduler.schedule(() -> processAccessRequest(requestId), delay);
        log.info("Scheduled delayed processing for access request {} with delay {}", requestId, delay);
    }

    @Job(name = "Process Batch of Access Requests")
    public void processBatch(int batchSize) {
        log.info("JobRunr: Processing batch of {} requests", batchSize);
        accessRequestService.processNextBatch(batchSize);
    }

    @Job(name = "Retry Failed Access Requests")
    public void retryFailedRequests() {
        log.info("JobRunr: Retrying failed requests");
        int scheduled = accessRequestService.retryFailedRequests();
        log.info("JobRunr: Scheduled {} requests for retry", scheduled);
    }
}
