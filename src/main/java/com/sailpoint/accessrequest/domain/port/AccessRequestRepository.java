package com.sailpoint.accessrequest.domain.port;

import com.sailpoint.accessrequest.domain.model.AccessRequest;
import com.sailpoint.accessrequest.domain.model.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface AccessRequestRepository {
    AccessRequest save(AccessRequest accessRequest);
    Optional<AccessRequest> findById(Long id);
    List<AccessRequest> findByStatus(RequestStatus status);
    List<AccessRequest> findFailedOrNonCompleted();
    List<AccessRequest> findAndLockForProcessing(int limit);
    void updateStatus(Long id, RequestStatus status, String errorMessage);
    void incrementRetryCount(Long id);
}
