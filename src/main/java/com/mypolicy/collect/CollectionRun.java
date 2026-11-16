package com.mypolicy.collect;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.mypolicy.policy.Region;

@Entity
@Table(name = "collection_run")
public class CollectionRun {

    public enum Status { RUNNING, SUCCESS, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.RUNNING;

    @Column(name = "first_run", nullable = false)
    private boolean firstRun;

    @Column(nullable = false)
    private int fetched;

    @Column(nullable = false)
    private int created;

    @Column(nullable = false)
    private int updated;

    @Column(nullable = false)
    private int cancelled;

    @Column(columnDefinition = "text")
    private String error;

    /** The service regions this run collected, comma-separated Region names. */
    @Column(length = 100)
    private String regions;

    protected CollectionRun() {
    }

    public CollectionRun(String source, Instant startedAt, boolean firstRun, Set<Region> regions) {
        this.source = source;
        this.startedAt = startedAt;
        this.firstRun = firstRun;
        this.regions = regions.stream().sorted().map(Region::name).collect(Collectors.joining(","));
    }

    void succeed(Instant at, CollectResult result) {
        status = Status.SUCCESS;
        finishedAt = at;
        fetched = result.fetched();
        created = result.created();
        updated = result.updated();
        cancelled = result.cancelled();
    }

    void fail(Instant at, String message) {
        status = Status.FAILED;
        finishedAt = at;
        error = message;
    }

    public Long getId() { return id; }
    public String getSource() { return source; }
    public Status getStatus() { return status; }
    public boolean isFirstRun() { return firstRun; }
    public int getFetched() { return fetched; }
    public int getCreated() { return created; }
    public int getUpdated() { return updated; }
    public int getCancelled() { return cancelled; }
    public String getError() { return error; }

    public Set<Region> getRegions() {
        Set<Region> set = EnumSet.noneOf(Region.class);
        if (regions != null && !regions.isBlank()) {
            Arrays.stream(regions.split(",")).map(String::trim).map(Region::valueOf).forEach(set::add);
        }
        return set;
    }
}
