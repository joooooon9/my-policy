package com.mypolicy.collect;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
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
    private final boolean catchUpOnStart;

    public CollectionScheduler(List<PolicySource> sources, CollectionService collection,
            @Value("${my-policy.collect.catch-up-on-start:true}") boolean catchUpOnStart) {
        this.sources = sources;
        this.collection = collection;
        this.catchUpOnStart = catchUpOnStart;
    }

    /**
     * Right after start, a source that is behind (never collected, last run failed, a region newly turned on)
     * is collected at once instead of at the next hour. Off with my-policy.collect.catch-up-on-start=false.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUp() {
        if (!catchUpOnStart) {
            return;
        }
        for (PolicySource source : sources) {
            if (collection.behind(source)) {
                log.info("{} is behind (no success yet, last run failed or a new region): collecting now", source.name());
                collection.collect(source);
            }
        }
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
