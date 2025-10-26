package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client;

import com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client.dto.AccessRequestResponseDto;
import com.maxgarsaiz.sailpoint.infrastructure.adapter.out.client.dto.CreateAccessRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "sailpoint-client",
    url = "${sailpoint.client.base-url}"
)
public interface SailpointFeignClient {
    
    @PostMapping("/access-requests")
    AccessRequestResponseDto createAccessRequest(@RequestBody CreateAccessRequestDto request);
    
    @GetMapping("/access-requests/{requestId}")
    AccessRequestResponseDto getAccessRequest(@PathVariable String requestId);
}