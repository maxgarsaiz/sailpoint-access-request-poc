package com.sailpoint.accessrequest.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequest {
    private Long id;
    private String requesterId;
    private String requesterName;
    private String targetUserId;
    private String targetUserName;
    private List<String> accessItems;
    private RequestStatus status;
    private String sailpointRequestId;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;
}
