package com.mypolicy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled jobs (the hourly collection). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
