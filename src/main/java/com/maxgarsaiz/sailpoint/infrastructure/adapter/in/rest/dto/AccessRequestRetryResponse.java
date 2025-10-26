package com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest.dto;

import com.maxgarsaiz.sailpoint.domain.port.in.RetryAccessRequestsUseCase.RetryResult;

import java.util.UUID;

public record AccessRequestRetryResponse(
    UUID accessRequestId,
    boolean success,
    String message
) {
    public static AccessRequestRetryResponse from(RetryResult result) {
        return new AccessRequestRetryResponse(
            result.accessRequestId(),
            result.success(),
            result.message()
        );
    }
}