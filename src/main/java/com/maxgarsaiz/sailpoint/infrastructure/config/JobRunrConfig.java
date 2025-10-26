package com.maxgarsaiz.sailpoint.infrastructure.config;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

import javax.sql.DataSource;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobRunrConfig {

    private final AccessRequestJobFilter accessRequestJobFilter;
    private final PollingConfigurationProperties pollingConfig;
    
    @Bean
    @ConditionalOnMissingBean
    public StorageProvider storageProvider(DataSource dataSource, JobMapper jobMapper) {
        PostgresStorageProvider storageProvider = new PostgresStorageProvider(dataSource);
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }

    @Bean
    public JobScheduler jobScheduler(StorageProvider storageProvider, JobActivator jobActivator, JsonMapper jsonMapper) {

        log.info("🔧 Configuring JobRunr with custom filters and settings");
        log.info("   - Max retries: {}", pollingConfig.getMaxRetries());
        log.info("   - Retry interval: {}s", pollingConfig.getRetryIntervalSeconds());
        
        boolean isBackgroundJobServerEnabled = true;
        boolean isDashboardEnabled = true;
        
        // Configure retry filter with custom settings
        var retryFilter = new RetryFilter(pollingConfig.getMaxRetries());
        
        JobScheduler jobScheduler = JobRunr.configure()
            .useJobActivator(jobActivator)
            .useJsonMapper(jsonMapper)
            .useStorageProvider(storageProvider)
            .withJobFilter(accessRequestJobFilter, retryFilter)
            .useBackgroundJobServerIf(
                isBackgroundJobServerEnabled, 
                usingStandardBackgroundJobServerConfiguration()
                    .andWorkerCount(4)
                    .andPollIntervalInSeconds(pollingConfig.getRetryIntervalSeconds()))
            .useDashboardIf(isDashboardEnabled, 8000)
            .initialize()
            .getJobScheduler();
        
        log.info("✅ JobRunr configured successfully");
        
        return jobScheduler;
    }
}
