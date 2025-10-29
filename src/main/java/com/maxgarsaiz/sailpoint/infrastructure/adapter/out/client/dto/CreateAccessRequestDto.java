package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client.dto;

public record CreateAccessRequestDto(
    String userId,
    String justification
) {}