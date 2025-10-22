package com.sailpoint.accessrequest.infrastructure.client;

import com.sailpoint.accessrequest.domain.exception.SailpointClientException;
import com.sailpoint.accessrequest.domain.model.AccessRequest;
import com.sailpoint.accessrequest.domain.port.SailpointClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SailpointClientAdapter implements SailpointClient {

    private final SailpointFeignClient feignClient;

    @Override
    public String submitAccessRequest(AccessRequest accessRequest) {
        try {
            SailpointAccessRequestDto requestDto = SailpointAccessRequestDto.builder()
                    .requesterId(accessRequest.getRequesterId())
                    .requesterName(accessRequest.getRequesterName())
                    .targetUserId(accessRequest.getTargetUserId())
                    .targetUserName(accessRequest.getTargetUserName())
                    .accessItems(accessRequest.getAccessItems())
                    .build();

            SailpointResponseDto response = feignClient.submitAccessRequest(requestDto);
            
            if (response.getRequestId() == null || response.getRequestId().isBlank()) {
                throw new SailpointClientException("Sailpoint returned empty request ID");
            }
            
            log.info("Successfully submitted access request to Sailpoint. Request ID: {}", response.getRequestId());
            return response.getRequestId();
            
        } catch (FeignException e) {
            log.error("Failed to submit access request to Sailpoint", e);
            throw new SailpointClientException("Failed to submit access request: " + e.getMessage(), e);
        }
    }

    @Override
    public String checkRequestStatus(String sailpointRequestId) {
        try {
            SailpointResponseDto response = feignClient.getRequestStatus(sailpointRequestId);
            return response.getStatus();
        } catch (FeignException e) {
            log.error("Failed to check request status in Sailpoint", e);
            throw new SailpointClientException("Failed to check request status: " + e.getMessage(), e);
        }
    }
}
