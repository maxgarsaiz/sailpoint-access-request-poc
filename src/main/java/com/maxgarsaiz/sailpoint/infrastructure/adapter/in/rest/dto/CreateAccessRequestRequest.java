package com.maxgarsaiz.sailpoint.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAccessRequestRequest(
    @NotBlank(message = "User ID is required")
    String userId,
    
    @NotBlank(message = "Access type is required")
    String accessType,
    
    String justification
) {}