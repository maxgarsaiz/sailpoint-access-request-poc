package com.sailpoint.accessrequest.infrastructure.rest;

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
public class AccessRequestResponseDto {
    private Long id;
    private String requesterId;
    private String requesterName;
    private String targetUserId;
    private String targetUserName;
    private List<String> accessItems;
    private String status;
    private String sailpointRequestId;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime processedAt;
}
