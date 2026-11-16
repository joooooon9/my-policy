package com.mypolicy.collect;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mypolicy.policy.Policy;
import com.mypolicy.policy.PolicyRepository;
import com.mypolicy.policy.PolicyStatus;
import com.mypolicy.policy.Region;

/**
 * Writes one source's listing into the database in one transaction: finds each notice by source and id,
 * creates or refreshes it, stamps the check time and records what changed as events.
 * In a region the source collects for the first time (quietRegions) no NEW events are written, so a first
 * collection, of the source or of a newly added region, only fills the list.
 */
@Component
class PolicyCollector {

    private final PolicyRepository policies;
    private final PolicyEventRepository events;
    private final Clock clock;

    PolicyCollector(PolicyRepository policies, PolicyEventRepository events, Clock clock) {
        this.policies = policies;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public CollectResult apply(String source, List<SourcePolicy> listing, Long runId, Set<Region> quietRegions) {
        Instant now = clock.instant();
        int created = 0;
        int updated = 0;
        int cancelled = 0;
        Set<String> seen = new HashSet<>();
        for (SourcePolicy item : listing) {
            if (!seen.add(item.sourceId())) {
                continue; // a source listing the same notice twice in one page set
            }
            Policy policy = policies.findBySourceAndSourceId(source, item.sourceId()).orElse(null);
            if (policy == null) {
                policy = policies.save(fill(new Policy(source, item.sourceId(), item.region(), item.title(), item.summary()), item)
                        .markChecked(now));
                created++;
                if (item.cancelled()) {
                    policy.cancel();
                } else if (item.closed()) {
                    policy.close(); // stored so a later reopening is seen, but never announced as new
                } else if (!quietRegions.contains(item.region())) {
                    event(policy, PolicyEvent.Type.NEW, null, null, runId, now);
                }
                continue;
            }

            LocalDate previousEnd = policy.getApplyEnd();
            LocalTime previousEndTime = policy.getApplyEndTime();
            boolean changed = !Content.of(policy).equals(Content.of(item));
            if (changed) {
                fill(policy, item);
                updated++;
            }
            policy.markChecked(now);

            if (item.closed() && !item.cancelled()) {
                policy.close();
            } else if (!item.closed()) {
                policy.reopen();
            }

            if (policy.getStatus() == PolicyStatus.ACTIVE && item.cancelled()) {
                policy.cancel();
                cancelled++;
                event(policy, PolicyEvent.Type.CANCELLED, previousEnd, previousEndTime, runId, now);
            } else if (policy.getStatus() == PolicyStatus.ACTIVE
                    && (!Objects.equals(previousEnd, item.applyEnd()) || !Objects.equals(previousEndTime, item.applyEndTime()))) {
                event(policy, PolicyEvent.Type.DEADLINE_CHANGED, previousEnd, previousEndTime, runId, now);
            }
        }
        return new CollectResult(seen.size(), created, updated, cancelled);
    }

    private void event(Policy policy, PolicyEvent.Type type, LocalDate previousEnd, LocalTime previousEndTime,
            Long runId, Instant now) {
        if (type != PolicyEvent.Type.DEADLINE_CHANGED && events.existsByPolicyIdAndType(policy.getId(), type)) {
            return; // NEW and CANCELLED happen once per notice (also guarded by a unique index)
        }
        events.save(new PolicyEvent(policy.getId(), type, previousEnd, previousEndTime,
                policy.getApplyEnd(), policy.getApplyEndTime(), runId, now));
    }

    private static Policy fill(Policy policy, SourcePolicy item) {
        return policy.withContent(item.region(), item.title(), item.summary())
                .withInstitution(item.institution())
                .withSupportContent(item.supportContent())
                .withUnemployedOnly(item.unemployedOnly())
                .withBenefit(item.benefit())
                .withAgeRange(item.minAge(), item.maxAge())
                .withEmployment(item.employedOnly(), item.smallBusinessOnly())
                .withApplyPeriod(item.applyStart(), item.applyEnd(), item.applyEndTime())
                .withLinks(item.officialUrl(), item.applyUrl())
                .withCheckNotes(item.checkNotes());
    }

    /** The stored content of a notice, to tell a real change from a repeated listing. */
    private record Content(Region region, String institution, String title, String summary, String supportContent,
            String benefit, Integer minAge, Integer maxAge, boolean employedOnly, boolean smallBusinessOnly,
            boolean unemployedOnly, LocalDate applyStart, LocalDate applyEnd,
            LocalTime applyEndTime, String officialUrl, String applyUrl, List<String> checkNotes) {

        static Content of(Policy p) {
            return new Content(p.getRegion(), p.getInstitution(), p.getTitle(), p.getSummary(), p.getSupportContent(),
                    p.getBenefit(), p.getMinAge(), p.getMaxAge(), p.isEmployedOnly(), p.isSmallBusinessOnly(),
                    p.isUnemployedOnly(), p.getApplyStart(), p.getApplyEnd(), p.getApplyEndTime(),
                    p.getOfficialUrl(), p.getApplyUrl(), p.getCheckNotes());
        }

        static Content of(SourcePolicy s) {
            return new Content(s.region(), s.institution(), s.title(), s.summary(), s.supportContent(),
                    s.benefit(), s.minAge(), s.maxAge(), s.employedOnly() || s.smallBusinessOnly(), s.smallBusinessOnly(),
                    s.unemployedOnly(), s.applyStart(), s.applyEnd(),
                    s.applyEndTime(), s.officialUrl(), s.applyUrl(), s.checkNotes());
        }
    }
}
