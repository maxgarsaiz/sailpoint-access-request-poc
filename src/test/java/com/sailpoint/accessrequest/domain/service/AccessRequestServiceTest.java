package com.sailpoint.accessrequest.domain.service;

import com.sailpoint.accessrequest.domain.exception.AccessRequestNotFoundException;
import com.sailpoint.accessrequest.domain.exception.InvalidAccessRequestException;
import com.sailpoint.accessrequest.domain.model.AccessRequest;
import com.sailpoint.accessrequest.domain.model.RequestStatus;
import com.sailpoint.accessrequest.domain.port.AccessRequestRepository;
import com.sailpoint.accessrequest.domain.port.SailpointClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessRequestServiceTest {

    @Mock
    private AccessRequestRepository repository;

    @Mock
    private SailpointClient sailpointClient;

    @InjectMocks
    private AccessRequestService service;

    private AccessRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = AccessRequest.builder()
                .requesterId("user123")
                .requesterName("John Doe")
                .targetUserId("target456")
                .targetUserName("Jane Smith")
                .accessItems(List.of("Role_A", "Role_B"))
                .build();
    }

    @Test
    void createAccessRequest_withValidRequest_shouldSaveAndReturnRequest() {
        // Given
        AccessRequest savedRequest = AccessRequest.builder()
                .id(1L)
                .requesterId("user123")
                .requesterName("John Doe")
                .targetUserId("target456")
                .targetUserName("Jane Smith")
                .accessItems(List.of("Role_A", "Role_B"))
                .status(RequestStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(repository.save(any(AccessRequest.class))).thenReturn(savedRequest);

        // When
        AccessRequest result = service.createAccessRequest(validRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(RequestStatus.PENDING, result.getStatus());
        assertEquals(0, result.getRetryCount());
        verify(repository).save(any(AccessRequest.class));
    }

    @Test
    void createAccessRequest_withNullRequesterId_shouldThrowException() {
        // Given
        validRequest.setRequesterId(null);

        // When & Then
        assertThrows(InvalidAccessRequestException.class, 
                () -> service.createAccessRequest(validRequest));
        verify(repository, never()).save(any());
    }

    @Test
    void createAccessRequest_withEmptyAccessItems_shouldThrowException() {
        // Given
        validRequest.setAccessItems(List.of());

        // When & Then
        assertThrows(InvalidAccessRequestException.class, 
                () -> service.createAccessRequest(validRequest));
        verify(repository, never()).save(any());
    }

    @Test
    void processAccessRequest_withValidPendingRequest_shouldProcessSuccessfully() {
        // Given
        AccessRequest pendingRequest = AccessRequest.builder()
                .id(1L)
                .requesterId("user123")
                .targetUserId("target456")
                .accessItems(List.of("Role_A"))
                .status(RequestStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AccessRequest processingRequest = AccessRequest.builder()
                .id(1L)
                .requesterId("user123")
                .targetUserId("target456")
                .accessItems(List.of("Role_A"))
                .status(RequestStatus.PROCESSING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(pendingRequest));
        when(repository.save(any(AccessRequest.class))).thenReturn(processingRequest);
        when(sailpointClient.submitAccessRequest(any())).thenReturn("SP-REQ-123");

        // When
        service.processAccessRequest(1L);

        // Then
        verify(repository, atLeastOnce()).findById(1L);
        verify(sailpointClient).submitAccessRequest(any());
        verify(repository, atLeast(2)).save(any(AccessRequest.class));
    }

    @Test
    void processAccessRequest_withNonExistentRequest_shouldThrowException() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AccessRequestNotFoundException.class, 
                () -> service.processAccessRequest(999L));
    }

    @Test
    void getAccessRequest_withExistingId_shouldReturnRequest() {
        // Given
        AccessRequest existingRequest = AccessRequest.builder()
                .id(1L)
                .requesterId("user123")
                .status(RequestStatus.COMPLETED)
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(existingRequest));

        // When
        AccessRequest result = service.getAccessRequest(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(RequestStatus.COMPLETED, result.getStatus());
    }

    @Test
    void getAccessRequest_withNonExistentId_shouldThrowException() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AccessRequestNotFoundException.class, 
                () -> service.getAccessRequest(999L));
    }

    @Test
    void retryFailedRequests_shouldScheduleRequestsForRetry() {
        // Given
        AccessRequest failedRequest1 = AccessRequest.builder()
                .id(1L)
                .status(RequestStatus.FAILED)
                .retryCount(1)
                .build();
        
        AccessRequest failedRequest2 = AccessRequest.builder()
                .id(2L)
                .status(RequestStatus.FAILED)
                .retryCount(2)
                .build();

        when(repository.findFailedOrNonCompleted()).thenReturn(List.of(failedRequest1, failedRequest2));
        when(repository.save(any(AccessRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int scheduled = service.retryFailedRequests();

        // Then
        assertEquals(2, scheduled);
        verify(repository, times(2)).save(any(AccessRequest.class));
    }
}
