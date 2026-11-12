package com.mypolicy.collect;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Checks every official source once an hour, on the hour in Korean time (my-policy.collect.cron).
 * Off with my-policy.collect.enabled=false. Runs one source after another; one failing does not stop the rest.
 */
@Component
@ConditionalOnProperty(name = "my-policy.collect.enabled", havingValue = "true", matchIfMissing = true)
public class CollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(CollectionScheduler.class);

    private final List<PolicySource> sources;
    private final CollectionService collection;

    public CollectionScheduler(List<PolicySource> sources, CollectionService collection) {
        this.sources = sources;
        this.collection = collection;
    }

    @Scheduled(cron = "${my-policy.collect.cron:0 0 * * * *}", zone = "${my-policy.zone:Asia/Seoul}")
    public void collectAll() {
        if (sources.isEmpty()) {
            log.debug("No official source is configured yet; nothing to collect");
            return;
        }
        for (PolicySource source : sources) {
            collection.collect(source);
        }
    }
}
