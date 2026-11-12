package com.mypolicy.collect;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import com.mypolicy.policy.Region;

/**
 * One notice as an official source lists it, already mapped to the service's terms.
 * applyEndTime is set only when the notice states a closing time. cancelled is true only when the
 * source itself says recruitment was cancelled; a notice missing from a listing is not taken as cancelled.
 */
public record SourcePolicy(String sourceId, Region region, String title, String summary, String benefit,
        Integer minAge, Integer maxAge, boolean employedOnly, boolean smallBusinessOnly,
        LocalDate applyStart, LocalDate applyEnd, LocalTime applyEndTime,
        String officialUrl, String applyUrl, List<String> checkNotes, boolean cancelled) {

    public SourcePolicy {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(title, "title");
        summary = summary == null ? "" : summary;
        checkNotes = checkNotes == null ? List.of() : List.copyOf(checkNotes);
    }
}
