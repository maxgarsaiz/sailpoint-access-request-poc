package com.maxgarsaiz.sailpoint.domain.service;

import com.maxgarsaiz.sailpoint.domain.exception.EntityNotFoundException;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.port.in.RetryAccessRequestsUseCase;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestRepositoryPort;
import com.maxgarsaiz.sailpoint.domain.port.out.JobSchedulerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRequestRetryService implements RetryAccessRequestsUseCase {
    
    private final AccessRequestRepositoryPort repositoryPort;
    private final JobSchedulerPort jobSchedulerPort;
    
    @Override
    @Transactional(readOnly = true)
    public RetryResult retrySingle(UUID accessRequestId) {
        log.info("Scheduling retry for access request: {}", accessRequestId);
        
        try {
            // ✅ Solo verificamos que exista - el job decidirá qué hacer
            AccessRequest accessRequest = repositoryPort.findByIdOrThrow(accessRequestId);
            
            // ✅ Solo verificamos que NO esté completado (terminal state)
            if (accessRequest.isCompleted()) {
                log.info("Request {} already completed, skipping retry", accessRequestId);
                return new RetryResult(accessRequestId, false, "Request already completed");
            }
            
            // ✅ Programar el job directamente - él sabrá qué hacer
            jobSchedulerPort.schedulePoolingJob(accessRequestId);
            log.info("✅ Successfully scheduled pooling job for request: {}", accessRequestId);
            return new RetryResult(accessRequestId, true, "Pooling job scheduled");
            
        } catch (EntityNotFoundException e) {
            log.error("Access request not found: {}", accessRequestId);
            return new RetryResult(accessRequestId, false, "Request not found");
        } catch (Exception e) {
            log.error("Unexpected error scheduling retry for access request: {}", accessRequestId, e);
            return new RetryResult(accessRequestId, false, "Error: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public BulkRetryResult retryFailed() {
        log.info("Scheduling retry for all failed access requests");
        List<AccessRequest> failedRequests = repositoryPort.findByStatus(AccessRequestStatus.PROCESSING_REQUIRES_ATTENTION);
        return processBulkRetry(failedRequests);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BulkRetryResult retryNonCompleted() {
        log.info("Scheduling retry for all non-completed access requests");
        List<AccessRequest> nonCompletedRequests = repositoryPort.findByStatusIn(
            AccessRequestStatus.getNonCompletedStatuses()
        );
        return processBulkRetry(nonCompletedRequests);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BulkRetryResult retryByFilters(RetryFilters filters) {
        log.info("Scheduling retry for access requests by filters: {}", filters);
        List<AccessRequest> filteredRequests = repositoryPort.findByFilters(filters);
        return processBulkRetry(filteredRequests);
    }
    
    @Override
    @Transactional(readOnly = true)
    public BulkRetryResult retryPooling() {
        log.info("Scheduling retry for all pooling access requests");
        List<AccessRequest> poolingRequests = repositoryPort.findByStatus(AccessRequestStatus.PROCESSING_IN_PROGRESS);
        return processBulkRetry(poolingRequests);
    }
    
    private BulkRetryResult processBulkRetry(List<AccessRequest> requests) {
        if (requests.isEmpty()) {
            log.info("No requests found to retry");
            return new BulkRetryResult(0, 0, 0, List.of());
        }
        
        log.info("Processing bulk retry for {} requests", requests.size());
        List<RetryResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        
        for (AccessRequest request : requests) {
            RetryResult result = retrySingle(request.getId());
            results.add(result);
            
            if (result.success()) {
                successCount++;
            } else {
                failedCount++;
            }
        }
        
        log.info("📊 Bulk retry completed - Total: {}, Success: {}, Failed: {}", 
            requests.size(), successCount, failedCount);
        
        return new BulkRetryResult(requests.size(), successCount, failedCount, results);
    }
}
