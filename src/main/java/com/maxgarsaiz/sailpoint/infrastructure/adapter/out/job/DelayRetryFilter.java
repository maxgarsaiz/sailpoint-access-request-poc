package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.job;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.RetryFilter;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom retry filter that applies a FIXED delay between retries.
 * Unlike JobRunr's default RetryFilter which uses exponential backoff,
 * this filter always waits the same amount of time between retry attempts.
 */
@Slf4j
public class DelayRetryFilter extends RetryFilter {
    
    private final int delaySeconds;
    
    /**
     * @param maxRetries Maximum number of retry attempts
     * @param delaySeconds Fixed delay in seconds between each retry (NOT exponential)
     */
    public DelayRetryFilter(int maxRetries, int delaySeconds) {
        super(maxRetries);
        this.delaySeconds = delaySeconds;
    }
    
    @Override
    protected long getSecondsToAdd(Job job) {
        return delaySeconds;
    }
}
