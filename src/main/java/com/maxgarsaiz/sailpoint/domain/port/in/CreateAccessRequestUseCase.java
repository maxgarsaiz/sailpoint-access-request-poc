package com.maxgarsaiz.sailpoint.domain.port.in;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;

public interface CreateAccessRequestUseCase {
    
    record CreateAccessRequestCommand(
        String userId,
        String accessType,
        String justification
    ) {}
    
    AccessRequest createAccessRequest(CreateAccessRequestCommand command);
}