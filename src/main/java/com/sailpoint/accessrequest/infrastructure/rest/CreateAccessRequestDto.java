package com.sailpoint.accessrequest.infrastructure.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccessRequestDto {
    
    @NotBlank(message = "Requester ID is required")
    private String requesterId;
    
    private String requesterName;
    
    @NotBlank(message = "Target user ID is required")
    private String targetUserId;
    
    private String targetUserName;
    
    @NotEmpty(message = "Access items are required")
    private List<String> accessItems;
}
