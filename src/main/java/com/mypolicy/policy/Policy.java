package com.mypolicy.policy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** One official policy notice, found again by its source and the source's own id. */
@Entity
@Table(name = "policy")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "source_id", nullable = false, length = 200)
    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Region region;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(length = 200)
    private String benefit;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "employed_only", nullable = false)
    private boolean employedOnly;

    @Column(name = "small_business_only", nullable = false)
    private boolean smallBusinessOnly;

    @Column(name = "apply_start")
    private LocalDate applyStart;

    @Column(name = "apply_end")
    private LocalDate applyEnd;

    /** Only when the official notice states a closing time. */
    @Column(name = "apply_end_time")
    private LocalTime applyEndTime;

    @Column(name = "official_url", length = 1000)
    private String officialUrl;

    @Column(name = "apply_url", length = 1000)
    private String applyUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Conditions the search form cannot decide (income, tenure, ...): shown to the visitor, never used to exclude. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "policy_check_note", joinColumns = @JoinColumn(name = "policy_id"))
    @OrderColumn(name = "position")
    @Column(name = "note", nullable = false, length = 200)
    private List<String> checkNotes = new ArrayList<>();

    protected Policy() {
    }

    public Policy(String source, String sourceId, Region region, String title, String summary) {
        this.source = source;
        this.sourceId = sourceId;
        this.region = region;
        this.title = title;
        this.summary = summary;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Policy withBenefit(String benefit) {
        this.benefit = benefit;
        return this;
    }

    public Policy withAgeRange(Integer minAge, Integer maxAge) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        return this;
    }

    public Policy withEmployment(boolean employedOnly, boolean smallBusinessOnly) {
        this.employedOnly = employedOnly || smallBusinessOnly;
        this.smallBusinessOnly = smallBusinessOnly;
        return this;
    }

    public Policy withApplyPeriod(LocalDate applyStart, LocalDate applyEnd, LocalTime applyEndTime) {
        this.applyStart = applyStart;
        this.applyEnd = applyEnd;
        this.applyEndTime = applyEndTime;
        return this;
    }

    public Policy withLinks(String officialUrl, String applyUrl) {
        this.officialUrl = officialUrl;
        this.applyUrl = applyUrl;
        return this;
    }

    public Policy withCheckNotes(List<String> notes) {
        this.checkNotes.clear();
        this.checkNotes.addAll(notes);
        return this;
    }

    public Policy markChecked(Instant at) {
        this.lastCheckedAt = at;
        return this;
    }

    public void cancel() {
        this.status = PolicyStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public String getSource() { return source; }
    public String getSourceId() { return sourceId; }
    public Region getRegion() { return region; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getBenefit() { return benefit; }
    public Integer getMinAge() { return minAge; }
    public Integer getMaxAge() { return maxAge; }
    public boolean isEmployedOnly() { return employedOnly; }
    public boolean isSmallBusinessOnly() { return smallBusinessOnly; }
    public LocalDate getApplyStart() { return applyStart; }
    public LocalDate getApplyEnd() { return applyEnd; }
    public LocalTime getApplyEndTime() { return applyEndTime; }
    public String getOfficialUrl() { return officialUrl; }
    public String getApplyUrl() { return applyUrl; }
    public PolicyStatus getStatus() { return status; }
    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public List<String> getCheckNotes() { return List.copyOf(checkNotes); }
}
