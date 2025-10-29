package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.persistence;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import com.maxgarsaiz.sailpoint.domain.port.in.RetryAccessRequestsUseCase.RetryFilters;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestRepositoryPort;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.out.persistence.mapper.JooqAccessRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JooqAccessRequesRepositoryAdapter implements AccessRequestRepositoryPort {
    
    private final DSLContext dsl;
    private final JooqAccessRequestMapper mapper;
    
    private static final String TABLE = "access_request";
    
    @Override
    public AccessRequest save(AccessRequest accessRequest) {
        log.debug("Saving access request: {}", accessRequest.getId());
        
        dsl.insertInto(table(TABLE))
            .columns(
                field("id"),
                field("user_id"),
                field("justification"),
                field("status"),
                field("sailpoint_request_id"),
                field("created_at"),
                field("updated_at")
            )
            .values(
                accessRequest.getId(),
                accessRequest.getUserId(),
                accessRequest.getJustification(),
                accessRequest.getStatus().name(),
                accessRequest.getSailpointRequestId(),
                accessRequest.getCreatedAt(),
                accessRequest.getUpdatedAt()
            )
            .execute();
        
        return accessRequest;
    }
    
    @Override
    public Optional<AccessRequest> findById(UUID id) {
        log.debug("Finding access request by id: {}", id);
        
        return dsl.selectFrom(table(TABLE))
            .where(field("id").eq(id))
            .fetchOptional()
            .map(mapper::toDomain);
    }

    @Override
    public List<AccessRequest> findAll() {
        log.debug("Finding all access requests");
        
        return dsl.selectFrom(table(TABLE))
            .fetch()
            .map(mapper::toDomain);
    }
    
    @Override
    public AccessRequest update(AccessRequest accessRequest) {
        log.debug("Updating access request: {}", accessRequest.getId());
        
        accessRequest.setUpdatedAt(LocalDateTime.now());
        
        dsl.update(table(TABLE))
            .set(field("user_id"), accessRequest.getUserId())
            .set(field("justification"), accessRequest.getJustification())
            .set(field("status"), accessRequest.getStatus().name())
            .set(field("sailpoint_request_id"), accessRequest.getSailpointRequestId())
            .set(field("updated_at"), accessRequest.getUpdatedAt())
            .where(field("id").eq(accessRequest.getId()))
            .execute();
        
        return accessRequest;
    }
    
    @Override
    public List<AccessRequest> findByStatus(AccessRequestStatus status) {
        log.debug("Finding access requests by status: {}", status);
        
        return dsl.selectFrom(table(TABLE))
            .where(field("status").eq(status.name()))
            .fetch()
            .map(mapper::toDomain);
    }
    
    @Override
    public List<AccessRequest> findByStatusIn(List<AccessRequestStatus> statuses) {
        log.debug("Finding access requests by statuses: {}", statuses);
        
        List<String> statusNames = statuses.stream()
            .map(AccessRequestStatus::name)
            .toList();
        
        return dsl.selectFrom(table(TABLE))
            .where(field("status").in(statusNames))
            .fetch()
            .map(mapper::toDomain);
    }
    
    @Override
    public List<AccessRequest> findByFilters(RetryFilters filters) {
        log.debug("Finding access requests by filters: {}", filters);
        
        Condition condition = trueCondition();
        
        if (filters.statuses() != null && !filters.statuses().isEmpty()) {
            List<String> statusNames = filters.statuses().stream()
                .map(AccessRequestStatus::name)
                .toList();
            condition = condition.and(field("status").in(statusNames));
        }
        
        if (filters.updatedBefore() != null) {
            condition = condition.and(field("updated_at").lt(filters.updatedBefore()));
        }
        
        if (filters.userId() != null) {
            condition = condition.and(field("user_id").eq(filters.userId()));
        }
        
        return dsl.selectFrom(table(TABLE))
            .where(condition)
            .fetch()
            .map(mapper::toDomain);
    }
    
    @Override
    public boolean transitionToPendingForRetry(UUID id) {
        log.debug("Transitioning access request to PENDING for retry: {}", id);
        
        int updated = dsl.update(table(TABLE))
            .set(field("status"), AccessRequestStatus.PENDING.name())
            .set(field("updated_at"), LocalDateTime.now())
            .where(field("id").eq(id))
            .and(field("status").in(
                AccessRequestStatus.PROCESSING_REQUIRES_ATTENTION.name(),
                AccessRequestStatus.PROCESSING_IN_PROGRESS.name()
            ))
            .execute();
        
        return updated > 0;
    }
}
