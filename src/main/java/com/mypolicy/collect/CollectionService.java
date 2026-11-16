package com.mypolicy.collect;

import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mypolicy.policy.Region;

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

    /**
     * True when the source should be collected now rather than at the next hour: it never succeeded,
     * its last run failed, or it collects a region no successful run has collected yet.
     */
    public boolean behind(PolicySource source) {
        String name = source.name();
        Set<Region> collected = EnumSet.noneOf(Region.class);
        runs.findBySourceAndStatus(name, CollectionRun.Status.SUCCESS).forEach(r -> collected.addAll(r.getRegions()));
        boolean lastFailed = runs.findFirstBySourceOrderByIdDesc(name)
                .map(r -> r.getStatus() == CollectionRun.Status.FAILED).orElse(true);
        return lastFailed || collected.isEmpty() || !collected.containsAll(source.regions());
    }

    public CollectionRun collect(PolicySource source) {
        String name = source.name();
        Set<Region> regions = EnumSet.noneOf(Region.class);
        regions.addAll(source.regions());
        Set<Region> collectedBefore = EnumSet.noneOf(Region.class);
        runs.findBySourceAndStatus(name, CollectionRun.Status.SUCCESS).forEach(r -> collectedBefore.addAll(r.getRegions()));
        boolean firstRun = !runs.existsBySourceAndStatus(name, CollectionRun.Status.SUCCESS);
        // a region collected for the first time only fills the list, like a source's first run
        Set<Region> newRegions = EnumSet.copyOf(regions);
        newRegions.removeAll(collectedBefore);
        CollectionRun run = runs.save(new CollectionRun(name, clock.instant(), firstRun, regions));
        try {
            List<SourcePolicy> listing = source.fetch();
            CollectResult result = collector.apply(name, listing, run.getId(), newRegions);
            run.succeed(clock.instant(), result);
            log.info("Collected {}: {} listed, {} new, {} changed, {} cancelled{}", name, result.fetched(),
                    result.created(), result.updated(), result.cancelled(),
                    newRegions.isEmpty() ? "" : " (first collection of " + newRegions + ": no new-notice events there)");
        } catch (Exception e) {
            run.fail(clock.instant(), e.getClass().getSimpleName() + ": " + e.getMessage());
            log.warn("Collection of {} failed; nothing was changed and the next run tries again", name, e);
        }
        return runs.save(run);
    }
}
