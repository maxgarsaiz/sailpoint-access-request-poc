package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.persistence.mapper;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequest;
import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;
import org.jooq.Record;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class JooqAccessRequestMapper {
    
    public AccessRequest toDomain(Record record) {
        return AccessRequest.builder()
            .id((UUID) record.get("id"))
            .userId((String) record.get("user_id"))
            .justification((String) record.get("justification"))
            .status(AccessRequestStatus.valueOf((String) record.get("status")))
            .createdAt(toLocalDateTime(record.get("created_at")))
            .updatedAt(toLocalDateTime(record.get("updated_at")))
            .build();
    }
    
    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to LocalDateTime");
    }
}