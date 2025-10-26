package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.port.out.IdentityProviderPort;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client.dto.AccessRequestResponseDto;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client.dto.CreateAccessRequestDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
                accessRequest.getAccessType(),
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
    public Optional<RequestStatus> checkRequestStatus(String sailpointRequestId) {
        log.debug("Checking status in Sailpoint for request: {}", sailpointRequestId);
        
        try {
            AccessRequestResponseDto response = feignClient.getAccessRequest(sailpointRequestId);
            
            Status status = mapStatus(response.status());
            
            log.debug("Sailpoint status for {}: {}", sailpointRequestId, status);
            
            return Optional.of(new RequestStatus(
                response.requestId(),
                status,
                response.message()
            ));
            
        } catch (FeignException.NotFound e) {
            log.warn("Access request not found in Sailpoint: {}", sailpointRequestId);
            return Optional.empty();
            
        } catch (FeignException e) {
            log.error("Error checking status in Sailpoint for request: {}", 
                sailpointRequestId, e);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Unexpected error checking status for request: {}", 
                sailpointRequestId, e);
            return Optional.empty();
        }
    }
    
    private Status mapStatus(String sailpointStatus) {
        if (sailpointStatus == null) {
            return Status.PENDING;
        }
        
        return switch (sailpointStatus.toUpperCase()) {
            case "COMPLETED", "APPROVED" -> Status.COMPLETED;
            case "FAILED", "REJECTED", "CANCELLED" -> Status.FAILED;
            case "IN_PROGRESS", "PROCESSING" -> Status.IN_PROGRESS;
            default -> Status.PENDING;
        };
    }
}