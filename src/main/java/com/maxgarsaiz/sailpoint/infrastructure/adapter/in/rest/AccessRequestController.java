package com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.port.in.CreateAccessRequestUseCase;
import com.maxgarsaiz.sailpoint.domain.port.in.GetAccessRequestUseCase;
import com.maxgarsaiz.sailpoint.domain.port.in.RetryAccessRequestsUseCase;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest.dto.AccessRequestResponse;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest.dto.AccessRequestRetryResponse;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest.dto.CreateAccessRequestRequest;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest.dto.AccessRequestBulkRetryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/access-requests")
@RequiredArgsConstructor
public class AccessRequestController {
    
    private final CreateAccessRequestUseCase createAccessRequestUseCase;
    private final GetAccessRequestUseCase getAccessRequestUseCase;
    private final RetryAccessRequestsUseCase retryAccessRequestsUseCase;
    
    @PostMapping
    public ResponseEntity<AccessRequestResponse> createAccessRequest(
        @Valid @RequestBody CreateAccessRequestRequest request
    ) {
        log.info("REST: Creating access request for user: {}", request.userId());
        
        CreateAccessRequestUseCase.CreateAccessRequestCommand command = 
            new CreateAccessRequestUseCase.CreateAccessRequestCommand(
                request.userId(),
                request.accessType(),
                request.justification()
            );
        
        AccessRequest created = createAccessRequestUseCase.createAccessRequest(command);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(AccessRequestResponse.from(created));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AccessRequestResponse> getAccessRequest(@PathVariable UUID id) {
        log.info("REST: Getting access request: {}", id);
        
        AccessRequest accessRequest = getAccessRequestUseCase.getById(id);
        
        return ResponseEntity.ok(AccessRequestResponse.from(accessRequest));
    }
    
    @GetMapping
    public ResponseEntity<List<AccessRequestResponse>> getAllAccessRequests() {
        log.info("REST: Getting all access requests");
        
        List<AccessRequestResponse> responses = getAccessRequestUseCase.getAll()
            .stream()
            .map(AccessRequestResponse::from)
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<AccessRequestRetryResponse> retrySingle(@PathVariable UUID id) {
        log.info("REST: Retry requested for access request: {}", id);
        
        RetryAccessRequestsUseCase.RetryResult result = retryAccessRequestsUseCase.retrySingle(id);
        
        return ResponseEntity
            .status(result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
            .body(AccessRequestRetryResponse.from(result));
    }

    @PostMapping("/retry/failed")
    public ResponseEntity<AccessRequestBulkRetryResponse> retryFailed() {
        log.info("REST: Bulk retry requested for FAILED access requests");
        
        RetryAccessRequestsUseCase.BulkRetryResult result = retryAccessRequestsUseCase.retryFailed();
        
        return ResponseEntity.ok(AccessRequestBulkRetryResponse.from(result));
    }

    @PostMapping("/retry/non-completed")
    public ResponseEntity<AccessRequestBulkRetryResponse> retryNonCompleted() {
        log.info("REST: Bulk retry requested for non-completed access requests");
        
        RetryAccessRequestsUseCase.BulkRetryResult result = retryAccessRequestsUseCase.retryNonCompleted();
        
        return ResponseEntity.ok(AccessRequestBulkRetryResponse.from(result));
    }

    @PostMapping("/retry/pooling")
    public ResponseEntity<AccessRequestBulkRetryResponse> retryPooling() {
        log.info("REST: Bulk retry requested for POOLING access requests");
        
        RetryAccessRequestsUseCase.BulkRetryResult result = retryAccessRequestsUseCase.retryPooling();
        
        return ResponseEntity.ok(AccessRequestBulkRetryResponse.from(result));
    }
}