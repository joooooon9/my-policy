package com.mypolicy.collect;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import com.mypolicy.policy.Region;

/**
 * One notice as an official source lists it, already mapped to the service's terms.
 * applyEndTime is set only when the notice states a closing time. closed is the source saying recruitment
 * ended (it may reopen); cancelled is true only when the source itself says recruitment was cancelled.
 * A notice missing from a listing is neither.
 */
public record SourcePolicy(String sourceId, Region region, String institution, String title, String summary,
        String supportContent, String benefit, Integer minAge, Integer maxAge,
        boolean employedOnly, boolean smallBusinessOnly, boolean unemployedOnly,
        LocalDate applyStart, LocalDate applyEnd, LocalTime applyEndTime,
        String officialUrl, String applyUrl, List<String> checkNotes, boolean closed, boolean cancelled) {

    public SourcePolicy {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(title, "title");
        summary = summary == null ? "" : summary;
        checkNotes = checkNotes == null ? List.of() : List.copyOf(checkNotes);
    }
}
