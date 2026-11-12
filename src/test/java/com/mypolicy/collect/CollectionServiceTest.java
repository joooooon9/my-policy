package com.mypolicy.collect;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.mypolicy.policy.Policy;
import com.mypolicy.policy.PolicyRepository;
import com.mypolicy.policy.PolicyStatus;
import com.mypolicy.policy.Region;

/** The collector against a real PostgreSQL with the Flyway schema (needs Docker). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({ PolicyCollector.class, CollectionService.class, CollectionServiceTest.FixedClock.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED) // each collection commits like it does in the app
class CollectionServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-12T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    CollectionService collection;
    @Autowired
    PolicyRepository policies;
    @Autowired
    PolicyEventRepository events;
    @Autowired
    CollectionRunRepository runs;

    private final FakeSource source = new FakeSource();

    @BeforeEach
    void clean() {
        events.deleteAll();
        runs.deleteAll();
        policies.deleteAll();
        source.items.clear();
        source.fail = false;
    }

    @Test
    void theFirstRunFillsTheListWithoutNewNoticeEvents() {
        source.items.add(item("A", LocalDate.of(2026, 11, 20)));
        source.items.add(item("B", LocalDate.of(2026, 11, 30)));

        CollectionRun run = collection.collect(source);

        assertThat(run.getStatus()).isEqualTo(CollectionRun.Status.SUCCESS);
        assertThat(run.isFirstRun()).isTrue();
        assertThat(run.getCreated()).isEqualTo(2);
        assertThat(policies.count()).isEqualTo(2);
        assertThat(events.count()).isZero();
        assertThat(stored("A").getLastCheckedAt()).isEqualTo(Instant.parse("2026-11-12T03:00:00Z"));
    }

    @Test
    void aNoticeFirstSeenLaterIsNewOnceAcrossRetries() {
        source.items.add(item("A", LocalDate.of(2026, 11, 20)));
        collection.collect(source);

        source.items.add(item("B", LocalDate.of(2026, 11, 30)));
        CollectionRun second = collection.collect(source);
        collection.collect(source); // the same listing again, like a retry

        assertThat(second.isFirstRun()).isFalse();
        assertThat(second.getCreated()).isEqualTo(1);
        assertThat(types("B")).containsExactly(PolicyEvent.Type.NEW);
        assertThat(types("A")).isEmpty();
    }

    @Test
    void aMovedDeadlineIsRecordedWithTheOldAndNewDate() {
        source.items.add(item("A", LocalDate.of(2026, 11, 20)));
        collection.collect(source);

        source.items.set(0, item("A", LocalDate.of(2026, 11, 27)));
        CollectionRun run = collection.collect(source);
        collection.collect(source); // unchanged listing: no second event

        assertThat(run.getUpdated()).isEqualTo(1);
        List<PolicyEvent> changes = events.findByPolicyIdOrderByIdAsc(stored("A").getId());
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getType()).isEqualTo(PolicyEvent.Type.DEADLINE_CHANGED);
        assertThat(changes.get(0).getPreviousApplyEnd()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(changes.get(0).getApplyEnd()).isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(stored("A").getApplyEnd()).isEqualTo(LocalDate.of(2026, 11, 27));
    }

    @Test
    void aClosingTimeAddedByTheSourceCountsAsADeadlineChange() {
        source.items.add(item("A", LocalDate.of(2026, 11, 20)));
        collection.collect(source);

        source.items.set(0, withEndTime(item("A", LocalDate.of(2026, 11, 20)), LocalTime.of(18, 0)));
        collection.collect(source);

        assertThat(types("A")).containsExactly(PolicyEvent.Type.DEADLINE_CHANGED);
        assertThat(stored("A").getApplyEndTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void aCancellationIsRecordedOnceAndTheNoticeLeavesTheList() {
        source.items.add(item("A", LocalDate.of(2026, 11, 20)));
        collection.collect(source);

        source.items.set(0, cancelled(item("A", LocalDate.of(2026, 11, 20))));
        CollectionRun run = collection.collect(source);
        collection.collect(source);

        assertThat(run.getCancelled()).isEqualTo(1);
        assertThat(stored("A").getStatus()).isEqualTo(PolicyStatus.CANCELLED);
        assertThat(types("A")).containsExactly(PolicyEvent.Type.CANCELLED);
    }

    @Test
    void aNoticeMissingFromTheListingIsNotCancelled() {
        source.items.add(item("A", LocalDate.of(2026, 11, 20)));
        collection.collect(source);

        source.items.clear();
        collection.collect(source);

        assertThat(stored("A").getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(events.count()).isZero();
    }

    @Test
    void aFailedFetchChangesNothingAndTheNextRunIsStillTheFirst() {
        source.fail = true;
        CollectionRun failed = collection.collect(source);

        assertThat(failed.getStatus()).isEqualTo(CollectionRun.Status.FAILED);
        assertThat(failed.getError()).contains("API down");
        assertThat(policies.count()).isZero();

        source.fail = false;
        source.items.add(item("A", LocalDate.of(2026, 11, 20)));
        CollectionRun next = collection.collect(source);
        assertThat(next.isFirstRun()).isTrue();
        assertThat(events.count()).isZero();
    }

    private Policy stored(String sourceId) {
        return policies.findBySourceAndSourceId(FakeSource.NAME, sourceId).orElseThrow();
    }

    private List<PolicyEvent.Type> types(String sourceId) {
        return events.findByPolicyIdOrderByIdAsc(stored(sourceId).getId()).stream().map(PolicyEvent::getType).toList();
    }

    private static SourcePolicy item(String id, LocalDate end) {
        return new SourcePolicy(id, Region.NATIONWIDE, "공고 " + id, "요약", null, 19, 34, false, false,
                null, end, null, "https://example.org/" + id, null, List.of(), false);
    }

    private static SourcePolicy withEndTime(SourcePolicy s, LocalTime time) {
        return new SourcePolicy(s.sourceId(), s.region(), s.title(), s.summary(), s.benefit(), s.minAge(), s.maxAge(),
                s.employedOnly(), s.smallBusinessOnly(), s.applyStart(), s.applyEnd(), time, s.officialUrl(),
                s.applyUrl(), s.checkNotes(), s.cancelled());
    }

    private static SourcePolicy cancelled(SourcePolicy s) {
        return new SourcePolicy(s.sourceId(), s.region(), s.title(), s.summary(), s.benefit(), s.minAge(), s.maxAge(),
                s.employedOnly(), s.smallBusinessOnly(), s.applyStart(), s.applyEnd(), s.applyEndTime(), s.officialUrl(),
                s.applyUrl(), s.checkNotes(), true);
    }

    static class FakeSource implements PolicySource {
        static final String NAME = "fake";
        final List<SourcePolicy> items = new ArrayList<>();
        boolean fail;

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public List<SourcePolicy> fetch() {
            if (fail) {
                throw new IllegalStateException("API down");
            }
            return List.copyOf(items);
        }
    }
}
