package com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest.dto;

import com.maxgarsaiz.sailpoint.domain.port.in.RetryAccessRequestsUseCase.BulkRetryResult;
import java.util.List;

public record AccessRequestBulkRetryResponse(
    int totalProcessed,
    int successCount,
    int failedCount,
    List<AccessRequestRetryResponse> results
) {
    public static AccessRequestBulkRetryResponse from(BulkRetryResult result) {
        List<AccessRequestRetryResponse> responses = result.results().stream()
            .map(AccessRequestRetryResponse::from)
            .toList();
            
        return new AccessRequestBulkRetryResponse(
            result.totalProcessed(),
            result.successCount(),
            result.failedCount(),
            responses
        );
    }
}
