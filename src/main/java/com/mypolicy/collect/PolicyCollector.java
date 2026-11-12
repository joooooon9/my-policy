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
 * On the source's first run no NEW events are written, so the first collection only fills the list.
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
    public CollectResult apply(String source, List<SourcePolicy> listing, Long runId, boolean firstRun) {
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
                } else if (!firstRun) {
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
                .withBenefit(item.benefit())
                .withAgeRange(item.minAge(), item.maxAge())
                .withEmployment(item.employedOnly(), item.smallBusinessOnly())
                .withApplyPeriod(item.applyStart(), item.applyEnd(), item.applyEndTime())
                .withLinks(item.officialUrl(), item.applyUrl())
                .withCheckNotes(item.checkNotes());
    }

    /** The stored content of a notice, to tell a real change from a repeated listing. */
    private record Content(Region region, String title, String summary, String benefit, Integer minAge, Integer maxAge,
            boolean employedOnly, boolean smallBusinessOnly, LocalDate applyStart, LocalDate applyEnd,
            LocalTime applyEndTime, String officialUrl, String applyUrl, List<String> checkNotes) {

        static Content of(Policy p) {
            return new Content(p.getRegion(), p.getTitle(), p.getSummary(), p.getBenefit(), p.getMinAge(), p.getMaxAge(),
                    p.isEmployedOnly(), p.isSmallBusinessOnly(), p.getApplyStart(), p.getApplyEnd(), p.getApplyEndTime(),
                    p.getOfficialUrl(), p.getApplyUrl(), p.getCheckNotes());
        }

        static Content of(SourcePolicy s) {
            return new Content(s.region(), s.title(), s.summary(), s.benefit(), s.minAge(), s.maxAge(),
                    s.employedOnly() || s.smallBusinessOnly(), s.smallBusinessOnly(), s.applyStart(), s.applyEnd(),
                    s.applyEndTime(), s.officialUrl(), s.applyUrl(), s.checkNotes());
        }
    }
}
