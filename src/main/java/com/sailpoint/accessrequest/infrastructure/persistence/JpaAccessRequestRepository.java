package com.sailpoint.accessrequest.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaAccessRequestRepository extends JpaRepository<AccessRequestEntity, Long> {
    
    List<AccessRequestEntity> findByStatus(String status);
    
    @Query("SELECT a FROM AccessRequestEntity a WHERE a.status IN ('FAILED', 'PENDING', 'RETRY_SCHEDULED') ORDER BY a.createdAt ASC")
    List<AccessRequestEntity> findFailedOrNonCompleted();
    
    @Query(value = "SELECT * FROM access_requests WHERE status IN ('PENDING', 'RETRY_SCHEDULED') " +
                   "ORDER BY created_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED", 
           nativeQuery = true)
    List<AccessRequestEntity> findAndLockForProcessing(@Param("limit") int limit);
    
    @Modifying
    @Query("UPDATE AccessRequestEntity a SET a.retryCount = a.retryCount + 1 WHERE a.id = :id")
    void incrementRetryCount(@Param("id") Long id);
}
