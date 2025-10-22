package com.sailpoint.accessrequest.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sailpoint.accessrequest.domain.model.AccessRequest;
import com.sailpoint.accessrequest.domain.model.RequestStatus;
import com.sailpoint.accessrequest.domain.port.AccessRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessRequestRepositoryAdapter implements AccessRequestRepository {

    private final JpaAccessRequestRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AccessRequest save(AccessRequest accessRequest) {
        AccessRequestEntity entity = toEntity(accessRequest);
        AccessRequestEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccessRequest> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessRequest> findByStatus(RequestStatus status) {
        return jpaRepository.findByStatus(status.name())
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccessRequest> findFailedOrNonCompleted() {
        return jpaRepository.findFailedOrNonCompleted()
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AccessRequest> findAndLockForProcessing(int limit) {
        return jpaRepository.findAndLockForProcessing(limit)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateStatus(Long id, RequestStatus status, String errorMessage) {
        jpaRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(status.name());
            entity.setErrorMessage(errorMessage);
            entity.setUpdatedAt(LocalDateTime.now());
            jpaRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public void incrementRetryCount(Long id) {
        jpaRepository.incrementRetryCount(id);
    }

    private AccessRequestEntity toEntity(AccessRequest domain) {
        String accessItemsJson;
        try {
            accessItemsJson = objectMapper.writeValueAsString(domain.getAccessItems());
        } catch (JsonProcessingException e) {
            log.error("Error serializing access items", e);
            accessItemsJson = "[]";
        }

        return AccessRequestEntity.builder()
                .id(domain.getId())
                .requesterId(domain.getRequesterId())
                .requesterName(domain.getRequesterName())
                .targetUserId(domain.getTargetUserId())
                .targetUserName(domain.getTargetUserName())
                .accessItems(accessItemsJson)
                .status(domain.getStatus() != null ? domain.getStatus().name() : RequestStatus.PENDING.name())
                .sailpointRequestId(domain.getSailpointRequestId())
                .errorMessage(domain.getErrorMessage())
                .retryCount(domain.getRetryCount() != null ? domain.getRetryCount() : 0)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .processedAt(domain.getProcessedAt())
                .build();
    }

    private AccessRequest toDomain(AccessRequestEntity entity) {
        List<String> accessItems;
        try {
            accessItems = Arrays.asList(objectMapper.readValue(entity.getAccessItems(), String[].class));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing access items", e);
            accessItems = List.of();
        }

        return AccessRequest.builder()
                .id(entity.getId())
                .requesterId(entity.getRequesterId())
                .requesterName(entity.getRequesterName())
                .targetUserId(entity.getTargetUserId())
                .targetUserName(entity.getTargetUserName())
                .accessItems(accessItems)
                .status(RequestStatus.valueOf(entity.getStatus()))
                .sailpointRequestId(entity.getSailpointRequestId())
                .errorMessage(entity.getErrorMessage())
                .retryCount(entity.getRetryCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .processedAt(entity.getProcessedAt())
                .build();
    }
}
