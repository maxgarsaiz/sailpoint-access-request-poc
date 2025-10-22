package com.sailpoint.accessrequest.infrastructure.config;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class JobRunrConfiguration {

    @Bean
    public org.jobrunr.storage.StorageProvider storageProvider(DataSource dataSource) {
        return SqlStorageProviderFactory.using(dataSource);
    }
}
