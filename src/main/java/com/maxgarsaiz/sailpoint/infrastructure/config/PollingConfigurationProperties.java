package com.maxgarsaiz.sailpoint.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import java.time.Duration;

/**
 * Configuration properties for Sailpoint access request polling.
 * Mapped from application.yml: sailpoint.pooling.*
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "sailpoint.pooling")
public class PollingConfigurationProperties {
    
    /**
     * Maximum number of polling attempts before marking request as failed.
     * Default: 3 attempts
     */
    @Min(1)
    private int maxRetries = 3;
    
    /**
     * Interval between polling attempts in seconds.
     * Default: 60 seconds (1 minute)
     */
    @Min(1)
    private int retryIntervalSeconds = 60;
    
    /**
     * Initial delay before first polling attempt in seconds.
     * Default: 5 seconds
     */
    @Min(0)
    private int initialDelaySeconds = 5;
    
    /**
     * Timeout for acquiring lock on access request (in minutes).
     * Default: 10 minutes
     */
    @Min(1)
    private int lockTimeoutMinutes = 10;
    
    /**
     * Get retry interval as Duration
     */
    public Duration getRetryInterval() {
        return Duration.ofSeconds(retryIntervalSeconds);
    }
    
    /**
     * Get initial delay as Duration
     */
    public Duration getInitialDelay() {
        return Duration.ofSeconds(initialDelaySeconds);
    }
    
    /**
     * Get lock timeout as Duration
     */
    public Duration getLockTimeout() {
        return Duration.ofMinutes(lockTimeoutMinutes);
    }
}