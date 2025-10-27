package com.maxgarsaiz.sailpoint.infrastructure.adapter.out.job;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.ApplyStateFilter;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ScheduledState;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * Custom retry filter that applies a FIXED delay between retries.
 * Unlike JobRunr's default RetryFilter which uses exponential backoff,
 * this filter always waits the same amount of time between retry attempts.
 */
@Slf4j
public class DelayRetryFilter extends RetryFilter {
    
    private final int fixedDelaySeconds;
    
    /**
     * @param maxRetries Maximum number of retry attempts
     * @param fixedDelaySeconds Fixed delay in seconds between each retry (NOT exponential)
     */
    public DelayRetryFilter(int maxRetries, int fixedDelaySeconds) {
        super(maxRetries);
        this.fixedDelaySeconds = fixedDelaySeconds;
    }
    
    @Override
    protected long getSecondsToAdd(Job job) {
      return fixedDelaySeconds;
   }
}
