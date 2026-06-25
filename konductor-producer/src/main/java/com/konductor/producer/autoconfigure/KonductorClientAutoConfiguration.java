package com.konductor.producer.autoconfigure;

import com.konductor.producer.HttpKonductorPublisher;
import com.konductor.producer.KonductorClientProperties;
import com.konductor.producer.KonductorPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(KonductorClientProperties.class)
public class KonductorClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KonductorPublisher konductorPublisher(KonductorClientProperties properties) {
        return new HttpKonductorPublisher(properties);
    }
}
