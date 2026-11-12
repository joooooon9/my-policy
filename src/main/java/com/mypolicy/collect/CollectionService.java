package com.mypolicy.collect;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Collects one source: records the run, fetches outside any transaction, applies the listing in one
 * transaction, and records success or the error. A failed run changes no notice, so the next run simply
 * tries again; a retry of a listing already applied finds nothing new to record.
 */
@Service
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    private final PolicyCollector collector;
    private final CollectionRunRepository runs;
    private final Clock clock;

    public CollectionService(PolicyCollector collector, CollectionRunRepository runs, Clock clock) {
        this.collector = collector;
        this.runs = runs;
        this.clock = clock;
    }

    public CollectionRun collect(PolicySource source) {
        String name = source.name();
        boolean firstRun = !runs.existsBySourceAndStatus(name, CollectionRun.Status.SUCCESS);
        CollectionRun run = runs.save(new CollectionRun(name, clock.instant(), firstRun));
        try {
            List<SourcePolicy> listing = source.fetch();
            CollectResult result = collector.apply(name, listing, run.getId(), firstRun);
            run.succeed(clock.instant(), result);
            log.info("Collected {}: {} listed, {} new, {} changed, {} cancelled{}", name, result.fetched(),
                    result.created(), result.updated(), result.cancelled(), firstRun ? " (first run, no new-notice events)" : "");
        } catch (Exception e) {
            run.fail(clock.instant(), e.getClass().getSimpleName() + ": " + e.getMessage());
            log.warn("Collection of {} failed; nothing was changed and the next run tries again", name, e);
        }
        return runs.save(run);
    }
}
