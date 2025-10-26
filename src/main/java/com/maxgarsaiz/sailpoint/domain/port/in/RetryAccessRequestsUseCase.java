package com.maxgarsaiz.sailpoint.domain.port.in;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RetryAccessRequestsUseCase {
    
    record RetryResult(
        UUID accessRequestId,
        boolean success,
        String message
    ) {}
    
    record BulkRetryResult(
        int totalProcessed,
        int successCount,
        int failedCount,
        List<RetryResult> results
    ) {}
    
    record RetryFilters(
        List<AccessRequestStatus> statuses,
        LocalDateTime updatedBefore,
        String userId
    ) {}
    
    RetryResult retrySingle(UUID accessRequestId);
    
    BulkRetryResult retryFailed();
    
    BulkRetryResult retryNonCompleted();
    
    BulkRetryResult retryByFilters(RetryFilters filters);
    
    BulkRetryResult retryPooling();
}