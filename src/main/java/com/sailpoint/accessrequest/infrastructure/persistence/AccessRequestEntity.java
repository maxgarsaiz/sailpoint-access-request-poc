package com.sailpoint.accessrequest.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "access_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequestEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "requester_id", nullable = false)
    private String requesterId;
    
    @Column(name = "requester_name")
    private String requesterName;
    
    @Column(name = "target_user_id", nullable = false)
    private String targetUserId;
    
    @Column(name = "target_user_name")
    private String targetUserName;
    
    @Column(name = "access_items", nullable = false, columnDefinition = "TEXT")
    private String accessItems; // JSON array as string
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private String status;
    
    @Column(name = "sailpoint_request_id")
    private String sailpointRequestId;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
