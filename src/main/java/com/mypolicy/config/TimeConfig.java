package com.mypolicy.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Application periods and status follow Korean time (my-policy.zone, Asia/Seoul). */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${my-policy.zone:Asia/Seoul}") ZoneId zone) {
        return Clock.system(zone);
    }
}
