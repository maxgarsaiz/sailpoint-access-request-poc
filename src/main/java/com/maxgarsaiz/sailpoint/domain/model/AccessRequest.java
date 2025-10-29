package com.maxgarsaiz.sailpoint.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequest {
    
    private UUID id;
    private String userId;
    private String justification;
    private String sailpointRequestId;
    private AccessRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Builder.Default
    private List<AccessRequestAttempt> attempts = new ArrayList<>();
    
    /**
     * Gets the last attempt (most recent by creation date).
     */
    public AccessRequestAttempt getLastAttempt() {
        return attempts.stream()
            .max(Comparator.comparing(AccessRequestAttempt::getCreatedAt))
            .orElse(null);
    }
    
    /**
     * Calculates the status based on the last attempt.
     * 
     * Rules:
     * - No attempts → PENDING
     * - Last attempt IN_PROGRESS → PROCESSING_IN_PROGRESS
     * - Last attempt COMPLETED → PROCESSING_COMPLETED
     * - Last attempt FAILED → PROCESSING_REQUIRES_ATTENTION
     */
    public AccessRequestStatus calculateStatus() {
        AccessRequestAttempt lastAttempt = getLastAttempt();
        
        if (lastAttempt == null) {
            return AccessRequestStatus.PENDING;
        }
        
        return switch (lastAttempt.getStatus()) {
            case IN_PROGRESS -> AccessRequestStatus.PROCESSING_IN_PROGRESS;
            case COMPLETED -> AccessRequestStatus.PROCESSING_COMPLETED;
            case FAILED -> AccessRequestStatus.PROCESSING_REQUIRES_ATTENTION;
        };
    }
    
    /**
     * Updates the status and timestamp.
     */
    public void updateStatus(AccessRequestStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isCompleted() {
        return status == AccessRequestStatus.PROCESSING_COMPLETED;
    }
}
