package com.sailpoint.accessrequest.infrastructure.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SailpointAccessRequestDto {
    private String requesterId;
    private String requesterName;
    private String targetUserId;
    private String targetUserName;
    private List<String> accessItems;
}
