package com.sailpoint.accessrequest.infrastructure.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SailpointResponseDto {
    private String requestId;
    private String status;
    private String message;
}
