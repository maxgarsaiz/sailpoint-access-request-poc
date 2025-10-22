package com.sailpoint.accessrequest.infrastructure.rest;

import com.sailpoint.accessrequest.domain.model.AccessRequest;
import com.sailpoint.accessrequest.domain.model.RequestStatus;
import com.sailpoint.accessrequest.domain.service.AccessRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/access-requests")
@RequiredArgsConstructor
public class AccessRequestController {

    private final AccessRequestService service;

    @PostMapping
    public ResponseEntity<AccessRequestResponseDto> createAccessRequest(
            @Valid @RequestBody CreateAccessRequestDto request) {
        
        log.info("Received create access request for target user: {}", request.getTargetUserId());
        
        AccessRequest domain = AccessRequest.builder()
                .requesterId(request.getRequesterId())
                .requesterName(request.getRequesterName())
                .targetUserId(request.getTargetUserId())
                .targetUserName(request.getTargetUserName())
                .accessItems(request.getAccessItems())
                .build();
        
        AccessRequest created = service.createAccessRequest(domain);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDto(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccessRequestResponseDto> getAccessRequest(@PathVariable Long id) {
        log.info("Fetching access request with id: {}", id);
        AccessRequest request = service.getAccessRequest(id);
        return ResponseEntity.ok(toDto(request));
    }

    @GetMapping
    public ResponseEntity<List<AccessRequestResponseDto>> getAccessRequestsByStatus(
            @RequestParam(required = false) String status) {
        
        List<AccessRequest> requests;
        if (status != null && !status.isBlank()) {
            log.info("Fetching access requests with status: {}", status);
            requests = service.getAccessRequestsByStatus(RequestStatus.valueOf(status.toUpperCase()));
        } else {
            log.info("Fetching all access requests");
            requests = service.getAccessRequestsByStatus(null);
        }
        
        return ResponseEntity.ok(requests.stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<Void> processAccessRequest(@PathVariable Long id) {
        log.info("Processing access request: {}", id);
        service.processAccessRequest(id);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/retry-failed")
    public ResponseEntity<RetryResultDto> retryFailedRequests() {
        log.info("Retrying failed requests");
        int scheduled = service.retryFailedRequests();
        return ResponseEntity.ok(new RetryResultDto(scheduled, "Scheduled " + scheduled + " requests for retry"));
    }

    @PostMapping("/process-batch")
    public ResponseEntity<BatchResultDto> processBatch(
            @RequestParam(defaultValue = "10") int batchSize) {
        
        log.info("Processing batch of size: {}", batchSize);
        List<AccessRequest> processed = service.processNextBatch(batchSize);
        
        return ResponseEntity.ok(new BatchResultDto(
                processed.size(),
                "Processed " + processed.size() + " requests"
        ));
    }

    private AccessRequestResponseDto toDto(AccessRequest domain) {
        return AccessRequestResponseDto.builder()
                .id(domain.getId())
                .requesterId(domain.getRequesterId())
                .requesterName(domain.getRequesterName())
                .targetUserId(domain.getTargetUserId())
                .targetUserName(domain.getTargetUserName())
                .accessItems(domain.getAccessItems())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .sailpointRequestId(domain.getSailpointRequestId())
                .errorMessage(domain.getErrorMessage())
                .retryCount(domain.getRetryCount())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .processedAt(domain.getProcessedAt())
                .build();
    }

    @Data
    @AllArgsConstructor
    public static class RetryResultDto {
        private int scheduledCount;
        private String message;
    }

    @Data
    @AllArgsConstructor
    public static class BatchResultDto {
        private int processedCount;
        private String message;
    }
}
