package com.mypolicy.collect;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A change in a notice that the notifications act on later. Written by the collector only. */
@Entity
@Table(name = "policy_event")
public class PolicyEvent {

    public enum Type {
        /** First seen after the source's first collection: a new-notice push. */
        NEW,
        /** The last day or closing time changed: tell interested members and reschedule deadline pushes. */
        DEADLINE_CHANGED,
        /** The source says recruitment was cancelled: tell interested members and drop deadline pushes. */
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(name = "previous_apply_end")
    private LocalDate previousApplyEnd;

    @Column(name = "previous_apply_end_time")
    private LocalTime previousApplyEndTime;

    @Column(name = "apply_end")
    private LocalDate applyEnd;

    @Column(name = "apply_end_time")
    private LocalTime applyEndTime;

    @Column(name = "collection_run_id")
    private Long collectionRunId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PolicyEvent() {
    }

    PolicyEvent(Long policyId, Type type, LocalDate previousApplyEnd, LocalTime previousApplyEndTime,
            LocalDate applyEnd, LocalTime applyEndTime, Long collectionRunId, Instant createdAt) {
        this.policyId = policyId;
        this.type = type;
        this.previousApplyEnd = previousApplyEnd;
        this.previousApplyEndTime = previousApplyEndTime;
        this.applyEnd = applyEnd;
        this.applyEndTime = applyEndTime;
        this.collectionRunId = collectionRunId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getPolicyId() { return policyId; }
    public Type getType() { return type; }
    public LocalDate getPreviousApplyEnd() { return previousApplyEnd; }
    public LocalTime getPreviousApplyEndTime() { return previousApplyEndTime; }
    public LocalDate getApplyEnd() { return applyEnd; }
    public LocalTime getApplyEndTime() { return applyEndTime; }
    public Long getCollectionRunId() { return collectionRunId; }
}
