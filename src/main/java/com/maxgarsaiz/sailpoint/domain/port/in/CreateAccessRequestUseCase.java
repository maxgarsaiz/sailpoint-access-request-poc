package com.maxgarsaiz.sailpoint.domain.port.in;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestAttempt;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.model.AttemptStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CreateAccessRequestUseCase {
    
    record CreateAccessRequestCommand(
        String userId,
        String accessType,
        String justification
    ) {
        /**
         * Converts this command to an AccessRequest domain entity.
         * 
         * @return a new AccessRequest with status PROCESSING_IN_PROGRESS
         */
        public AccessRequest toDomain() {
            return AccessRequest.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .justification(justification)
                .status(AccessRequestStatus.PROCESSING_IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        }
        
        /**
         * Creates the first AccessRequestAttempt for a given AccessRequest.
         * 
         * @param accessRequestId the ID of the parent AccessRequest
         * @return a new AccessRequestAttempt with status IN_PROGRESS
         */
        public AccessRequestAttempt toFirstAttempt(UUID accessRequestId) {
            return AccessRequestAttempt.builder()
                .id(UUID.randomUUID())
                .accessRequestId(accessRequestId)
                .status(AttemptStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        }
    }
    
    AccessRequest createAccessRequest(CreateAccessRequestCommand command);
}