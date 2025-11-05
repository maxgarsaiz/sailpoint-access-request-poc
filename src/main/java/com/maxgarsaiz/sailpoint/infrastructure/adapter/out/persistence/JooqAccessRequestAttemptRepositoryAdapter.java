package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.persistence;

import com.maxgarsaiz.sailpoint.domain.exception.EntityNotFoundException;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestAttempt;
import com.maxgarsaiz.sailpoint.domain.model.AttemptStatus;
import com.maxgarsaiz.sailpoint.domain.port.out.AccessRequestAttemptRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JooqAccessRequestAttemptRepositoryAdapter implements AccessRequestAttemptRepositoryPort {
    
    private final DSLContext dsl;
    
    private static final String TABLE = "access_request_attempt";
    
    /**
     * Helper method to map jOOQ Record to AccessRequestAttempt domain object.
     * Handles Timestamp to LocalDateTime conversion.
     */
    private AccessRequestAttempt mapRecordToAttempt(Record record) {
        return AccessRequestAttempt.builder()
            .id((UUID) record.get("id"))
            .accessRequestId((UUID) record.get("access_request_id"))
            .sailpointAccessRequestId((String) record.get("sailpoint_access_request_id"))
            .status(AttemptStatus.valueOf((String) record.get("status")))
            .errorMessage((String) record.get("error_message"))
            .createdAt(toLocalDateTime(record.get("created_at")))
            .updatedAt(toLocalDateTime(record.get("updated_at")))
            .completedAt(toLocalDateTime(record.get("completed_at")))
            .build();
    }
    
    /**
     * Converts Timestamp to LocalDateTime safely.
     */
    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to LocalDateTime");
    }
    
    @Override
    public AccessRequestAttempt save(AccessRequestAttempt attempt) {
        log.debug("Saving access request attempt: {}", attempt.getId());
        
        dsl.insertInto(table(TABLE),
                field("id"),
                field("access_request_id"),
                field("sailpoint_access_request_id"),
                field("status"),
                field("error_message"),
                field("created_at"),
                field("updated_at"),
                field("completed_at")
            )
            .values(
                attempt.getId(),
                attempt.getAccessRequestId(),
                attempt.getSailpointAccessRequestId(),
                attempt.getStatus().name(),
                attempt.getErrorMessage(),
                attempt.getCreatedAt(),
                attempt.getUpdatedAt(),
                attempt.getCompletedAt()
            )
            .execute();
        
        return attempt;
    }
    
    @Override
    public AccessRequestAttempt update(AccessRequestAttempt attempt) {
        log.debug("Updating access request attempt: {}", attempt.getId());
        
        attempt.setUpdatedAt(LocalDateTime.now());
        
        dsl.update(table(TABLE))
            .set(field("sailpoint_access_request_id"), attempt.getSailpointAccessRequestId())
            .set(field("status"), attempt.getStatus().name())
            .set(field("error_message"), attempt.getErrorMessage())
            .set(field("updated_at"), attempt.getUpdatedAt())
            .set(field("completed_at"), attempt.getCompletedAt())
            .where(field("id").eq(attempt.getId()))
            .execute();
        
        return attempt;
    }
    
    @Override
    public Optional<AccessRequestAttempt> findById(UUID id) {
        log.debug("Finding access request attempt by id: {}", id);
        
        return dsl.select()
            .from(table(TABLE))
            .where(field("id").eq(id))
            .fetchOptional()
            .map(this::mapRecordToAttempt);
    }
    
    @Override
    public AccessRequestAttempt findByIdOrThrow(UUID id) throws EntityNotFoundException {
        return findById(id)
            .orElseThrow(() -> new EntityNotFoundException("AccessRequestAttempt", id));
    }
    
    @Override
    public java.util.List<AccessRequestAttempt> findByAccessRequestId(UUID accessRequestId) {
        log.debug("Finding attempts by access request id: {}", accessRequestId);
        
        return dsl.select()
            .from(table(TABLE))
            .where(field("access_request_id").eq(accessRequestId))
            .orderBy(field("created_at").desc())
            .fetch()
            .map(this::mapRecordToAttempt);
    }
    
    @Override
    public Optional<AccessRequestAttempt> findLastByAccessRequestId(UUID accessRequestId) {
        log.debug("Finding last attempt by access request id: {}", accessRequestId);
        
        return dsl.select()
            .from(table(TABLE))
            .where(field("access_request_id").eq(accessRequestId))
            .orderBy(field("created_at").desc())
            .limit(1)
            .fetchOptional()
            .map(this::mapRecordToAttempt);
    }
    
    @Override
    public java.util.List<AccessRequestAttempt> findByStatus(AttemptStatus status) {
        log.debug("Finding attempts by status: {}", status);
        
        return dsl.select()
            .from(table(TABLE))
            .where(field("status").eq(status.name()))
            .orderBy(field("created_at").desc())
            .fetch()
            .map(this::mapRecordToAttempt);
    }
    
    @Override
    public void deleteById(UUID id) {
        log.debug("Deleting access request attempt: {}", id);
        
        dsl.deleteFrom(table(TABLE))
            .where(field("id").eq(id))
            .execute();
    }
}
