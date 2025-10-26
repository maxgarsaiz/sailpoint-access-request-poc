package com.maxgarsaiz.sailpoint.domain.port.out;

import java.util.UUID;

public interface JobSchedulerPort {
    
    void schedulePoolingJob(UUID accessRequestId);
}