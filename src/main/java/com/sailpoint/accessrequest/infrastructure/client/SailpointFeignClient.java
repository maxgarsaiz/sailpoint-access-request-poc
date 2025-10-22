package com.sailpoint.accessrequest.infrastructure.client;

import feign.Param;
import feign.RequestLine;

public interface SailpointFeignClient {
    
    @RequestLine("POST /api/v1/access-requests")
    SailpointResponseDto submitAccessRequest(SailpointAccessRequestDto request);
    
    @RequestLine("GET /api/v1/access-requests/{requestId}")
    SailpointResponseDto getRequestStatus(@Param("requestId") String requestId);
}
