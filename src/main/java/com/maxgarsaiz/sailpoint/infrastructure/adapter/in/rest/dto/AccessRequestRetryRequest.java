package com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest.dto;

import com.maxgarsaiz.sailpoint.domain.model.AccessRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AccessRequestRetryRequest(
    List<AccessRequestStatus> statuses,
    LocalDateTime updatedBefore,
    String userId
) {}