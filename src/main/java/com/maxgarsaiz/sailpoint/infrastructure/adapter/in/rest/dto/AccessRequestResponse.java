package com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest.dto;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccessRequestResponse(
    UUID id,
    String userId,
    String accessType,
    String justification,
    AccessRequestStatus status,
    String sailpointRequestId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AccessRequestResponse from(AccessRequest accessRequest) {
        return new AccessRequestResponse(
            accessRequest.getId(),
            accessRequest.getUserId(),
            accessRequest.getAccessType(),
            accessRequest.getJustification(),
            accessRequest.getStatus(),
            accessRequest.getSailpointRequestId(),
            accessRequest.getCreatedAt(),
            accessRequest.getUpdatedAt()
        );
    }
}