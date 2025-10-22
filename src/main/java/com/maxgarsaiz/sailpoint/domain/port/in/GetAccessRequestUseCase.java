package com.maxgarsaiz.sailpoint.domain.port.in;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;

import java.util.List;
import java.util.UUID;

public interface GetAccessRequestUseCase {
    
    AccessRequest getById(UUID id);
    
    List<AccessRequest> getAll();
}