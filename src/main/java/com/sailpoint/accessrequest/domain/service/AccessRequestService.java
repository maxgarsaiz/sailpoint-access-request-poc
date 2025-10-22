package com.sailpoint.accessrequest.domain.service;

import com.sailpoint.accessrequest.domain.exception.AccessRequestNotFoundException;
import com.sailpoint.accessrequest.domain.exception.InvalidAccessRequestException;
import com.sailpoint.accessrequest.domain.exception.SailpointClientException;
import com.sailpoint.accessrequest.domain.model.AccessRequest;
import com.sailpoint.accessrequest.domain.model.RequestStatus;
import com.sailpoint.accessrequest.domain.port.AccessRequestRepository;
import com.sailpoint.accessrequest.domain.port.SailpointClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRequestService {

    private final AccessRequestRepository repository;
    private final SailpointClient sailpointClient;

    @Transactional
    public AccessRequest createAccessRequest(AccessRequest request) {
        validateAccessRequest(request);
        
        request.setStatus(RequestStatus.PENDING);
        request.setRetryCount(0);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        
        AccessRequest saved = repository.save(request);
        log.info("Created access request with id: {}", saved.getId());
        
        return saved;
    }

    @Transactional
    public void processAccessRequest(Long requestId) {
        AccessRequest request = repository.findById(requestId)
                .orElseThrow(() -> new AccessRequestNotFoundException(requestId));

        if (request.getStatus() != RequestStatus.PENDING && 
            request.getStatus() != RequestStatus.RETRY_SCHEDULED) {
            log.warn("Request {} is not in processable state: {}", requestId, request.getStatus());
            return;
        }

        try {
            request.setStatus(RequestStatus.PROCESSING);
            request.setUpdatedAt(LocalDateTime.now());
            repository.save(request);

            String sailpointRequestId = sailpointClient.submitAccessRequest(request);
            
            request.setSailpointRequestId(sailpointRequestId);
            request.setStatus(RequestStatus.COMPLETED);
            request.setProcessedAt(LocalDateTime.now());
            request.setErrorMessage(null);
            
            repository.save(request);
            log.info("Successfully processed request {}", requestId);
            
        } catch (SailpointClientException e) {
            log.error("Failed to process request {}", requestId, e);
            request.setStatus(RequestStatus.FAILED);
            request.setErrorMessage(e.getMessage());
            request.setUpdatedAt(LocalDateTime.now());
            repository.save(request);
            repository.incrementRetryCount(requestId);
        }
    }

    @Transactional(readOnly = true)
    public AccessRequest getAccessRequest(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AccessRequestNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<AccessRequest> getAccessRequestsByStatus(RequestStatus status) {
        return repository.findByStatus(status);
    }

    @Transactional
    public int retryFailedRequests() {
        List<AccessRequest> failedRequests = repository.findFailedOrNonCompleted();
        log.info("Found {} failed/non-completed requests to retry", failedRequests.size());
        
        int scheduled = 0;
        for (AccessRequest request : failedRequests) {
            if (request.getRetryCount() < 3) {
                request.setStatus(RequestStatus.RETRY_SCHEDULED);
                request.setUpdatedAt(LocalDateTime.now());
                repository.save(request);
                scheduled++;
            }
        }
        
        log.info("Scheduled {} requests for retry", scheduled);
        return scheduled;
    }

    @Transactional
    public List<AccessRequest> processNextBatch(int batchSize) {
        List<AccessRequest> requests = repository.findAndLockForProcessing(batchSize);
        log.info("Processing batch of {} requests", requests.size());
        
        for (AccessRequest request : requests) {
            try {
                processAccessRequest(request.getId());
            } catch (Exception e) {
                log.error("Error processing request {} in batch", request.getId(), e);
            }
        }
        
        return requests;
    }

    private void validateAccessRequest(AccessRequest request) {
        if (request.getRequesterId() == null || request.getRequesterId().isBlank()) {
            throw new InvalidAccessRequestException("Requester ID is required");
        }
        if (request.getTargetUserId() == null || request.getTargetUserId().isBlank()) {
            throw new InvalidAccessRequestException("Target user ID is required");
        }
        if (request.getAccessItems() == null || request.getAccessItems().isEmpty()) {
            throw new InvalidAccessRequestException("Access items are required");
        }
    }
}
