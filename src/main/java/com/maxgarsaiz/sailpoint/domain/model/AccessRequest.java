package com.maxgarsaiz.sailpoint.domain.model;

import com.maxgarsaiz.sailpoint.domain.exception.InvalidStateTransitionException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequest {
    private UUID id;
    private String userId;
    private String accessType;
    private String justification;
    private AccessRequestStatus status;
    private String sailpointRequestId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void markAsPooling() {
        validateTransition(AccessRequestStatus.POOLING);
        this.status = AccessRequestStatus.POOLING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCompleted(String sailpointRequestId) {
        validateTransition(AccessRequestStatus.COMPLETED);
        this.status = AccessRequestStatus.COMPLETED;
        this.sailpointRequestId = sailpointRequestId;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        validateTransition(AccessRequestStatus.FAILED);
        this.status = AccessRequestStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public void transitionBackToPending() {
        validateTransition(AccessRequestStatus.PENDING);
        this.status = AccessRequestStatus.PENDING;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateTransition(AccessRequestStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot transition from %s to %s", this.status, newStatus)
            );
        }
    }
}