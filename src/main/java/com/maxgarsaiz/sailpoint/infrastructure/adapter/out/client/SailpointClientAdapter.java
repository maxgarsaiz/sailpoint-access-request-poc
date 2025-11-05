package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestAttempt;
import com.maxgarsaiz.sailpoint.domain.port.out.IdentityProviderPort;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client.dto.AccessRequestResponseDto;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client.dto.CreateAccessRequestDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SailpointClientAdapter implements IdentityProviderPort {
    
    private final SailpointFeignClient feignClient;
    
    @Override
    public AccessRequest createAccessRequest(AccessRequest accessRequest) 
            throws IdentityClientException {
        
        // Get the attempt from lastAttempt
        AccessRequestAttempt attempt = accessRequest.getLastAttempt();
        if (attempt == null) {
            throw new IdentityClientException(
                "AccessRequest must have lastAttempt set before calling createAccessRequest", null);
        }
        
        log.info("Creating access request attempt in Sailpoint for user: {} (attemptId: {})", 
                accessRequest.getUserId(), attempt.getId());
        
        try {
            CreateAccessRequestDto request = new CreateAccessRequestDto(
                accessRequest.getUserId(),
                accessRequest.getJustification());
            
            AccessRequestResponseDto response = feignClient.createAccessRequest(request);
            
            log.info("Successfully created Sailpoint request. SailpointID: {}, Status: {} (attemptId: {})", 
                response.requestId(), response.status(), attempt.getId());
            
            // Update the attempt with Sailpoint access request ID
            attempt.setSailpointAccessRequestId(response.requestId());
            
            return accessRequest;
            
        } catch (FeignException.BadRequest e) {
            log.error("Bad request when creating access request in Sailpoint for user: {} (attemptId: {})", 
                accessRequest.getUserId(), attempt.getId(), e);
            throw new IdentityClientException(
                "Invalid request to Sailpoint: " + e.contentUTF8(), e);
                
        } catch (FeignException.ServiceUnavailable | FeignException.GatewayTimeout e) {
            log.error("Sailpoint service unavailable for user: {} (attemptId: {})", 
                accessRequest.getUserId(), attempt.getId(), e);
            throw new IdentityClientException(
                "Sailpoint service is temporarily unavailable", e);
                
        } catch (FeignException e) {
            log.error("Unexpected Feign error creating access request for user: {} (attemptId: {})", 
                accessRequest.getUserId(), attempt.getId(), e);
            throw new IdentityClientException(
                "Failed to create access request in Sailpoint: " + e.getMessage(), e);
                
        } catch (Exception e) {
            log.error("Unexpected error creating access request for user: {} (attemptId: {})", 
                accessRequest.getUserId(), attempt.getId(), e);
            throw new IdentityClientException(
                "Unexpected error communicating with Sailpoint", e);
        }
    }

    @Override
    public SailpointStatusResponse getRequestStatus(AccessRequestAttempt attempt) throws IdentityClientException {
        String sailpointAccessRequestId = attempt.getSailpointAccessRequestId();
        log.info("Retrieving access request status from Sailpoint: {} (attemptId: {})", 
                sailpointAccessRequestId, attempt.getId());

        try {
            AccessRequestResponseDto response = feignClient.getAccessRequest(sailpointAccessRequestId);
            log.info("Successfully retrieved Sailpoint request status: {} (attemptId: {})", 
                    response.status(), attempt.getId());
            
            return new SailpointStatusResponse(response.status(), response.message());

        } catch (FeignException.NotFound e) {
            log.warn("Access request not found in Sailpoint: {} (attemptId: {})", 
                    sailpointAccessRequestId, attempt.getId());
            throw new IdentityClientException(
                "Access request not found in Sailpoint: " + sailpointAccessRequestId, e);
            
        } catch (FeignException e) {
            log.error("Error retrieving access request from Sailpoint: {} (attemptId: {})", 
                    sailpointAccessRequestId, attempt.getId(), e);
            throw new IdentityClientException(
                "Failed to retrieve access request from Sailpoint: " + e.getMessage(), e);
                
        } catch (Exception e) {
            log.error("Unexpected error retrieving access request from Sailpoint: {} (attemptId: {})", 
                    sailpointAccessRequestId, attempt.getId(), e);
            throw new IdentityClientException(
                "Unexpected error communicating with Sailpoint", e);
        }
    }
}