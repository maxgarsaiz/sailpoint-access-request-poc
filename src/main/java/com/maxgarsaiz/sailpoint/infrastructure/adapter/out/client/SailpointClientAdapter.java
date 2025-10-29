package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.port.out.IdentityProviderPort;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client.dto.AccessRequestResponseDto;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client.dto.CreateAccessRequestDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Date;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SailpointClientAdapter implements IdentityProviderPort {
    
    private final SailpointFeignClient feignClient;
    
    @Override
    public AccessRequest createAccessRequest(AccessRequest accessRequest) {
        log.info("Creating access request in Sailpoint for user: {}", accessRequest.getUserId());
        
        try {
            CreateAccessRequestDto request = new CreateAccessRequestDto(
                accessRequest.getUserId(),
                accessRequest.getJustification());
            
            AccessRequestResponseDto response = feignClient.createAccessRequest(request);
            
            log.info("Successfully created Sailpoint request. SailpointID: {}, Status: {}", 
                response.requestId(), response.status());
            
            // Update the domain model with Sailpoint request ID
            accessRequest.setSailpointRequestId(response.requestId());
            
            return accessRequest;
            
        } catch (FeignException.BadRequest e) {
            log.error("Bad request when creating access request in Sailpoint for user: {}", 
                accessRequest.getUserId(), e);
            throw new IdentityClientException(
                "Invalid request to Sailpoint: " + e.contentUTF8(), e);
                
        } catch (FeignException.ServiceUnavailable | FeignException.GatewayTimeout e) {
            log.error("Sailpoint service unavailable for user: {}", accessRequest.getUserId(), e);
            throw new IdentityClientException(
                "Sailpoint service is temporarily unavailable", e);
                
        } catch (FeignException e) {
            log.error("Unexpected Feign error creating access request for user: {}", 
                accessRequest.getUserId(), e);
            throw new IdentityClientException(
                "Failed to create access request in Sailpoint: " + e.getMessage(), e);
                
        } catch (Exception e) {
            log.error("Unexpected error creating access request for user: {}", 
                accessRequest.getUserId(), e);
            throw new IdentityClientException(
                "Unexpected error communicating with Sailpoint", e);
        }
    }

    @Override
    public SailpointStatusResponse getAccessRequest(String sailpointRequestId) {
        log.info("Retrieving access request from Sailpoint: {}", sailpointRequestId);

        try {
            AccessRequestResponseDto response = feignClient.getAccessRequest(sailpointRequestId);
            log.info("Successfully retrieved Sailpoint request: {}", response);
            return new SailpointStatusResponse(
                response.requestId(),
                mapStatus(response.status()),
                response.message()
            );

        } catch (FeignException.NotFound e) {
            log.warn("Access request not found in Sailpoint: {}", accessRequestId);
            return null;
            
        } catch (FeignException e) {
            log.error("Error retrieving access request from Sailpoint: {}", accessRequestId, e);
            throw new IdentityClientException(
                "Failed to retrieve access request from Sailpoint: " + e.getMessage(), e);
                
        } catch (Exception e) {
            log.error("Unexpected error retrieving access request from Sailpoint: {}", accessRequestId, e);
            throw new IdentityClientException(
                "Unexpected error communicating with Sailpoint", e);
        }
    }

    private AccessRequestStatus mapStatus(String sailpointStatus) {
        if (sailpointStatus == null) {
            return AccessRequestStatus.PENDING;
        }
        
        return switch (sailpointStatus.toUpperCase()) {
            case "COMPLETED" -> AccessRequestStatus.PROCESSING_COMPLETED;
            case "FAILED" -> AccessRequestStatus.PROCESSING_REQUIRES_ATTENTION;
            case "IN_PROGRESS" -> AccessRequestStatus.PROCESSING_IN_PROGRESS;
            default -> AccessRequestStatus.PENDING;
        };
    }
}