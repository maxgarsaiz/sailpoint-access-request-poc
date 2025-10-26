package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client.dto;

public record AccessRequestResponseDto(
    String requestId,
    String status,
    String message
) {}