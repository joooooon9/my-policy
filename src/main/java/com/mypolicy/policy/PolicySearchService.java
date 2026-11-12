package com.mypolicy.policy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listing, custom search and the detail page. The screens call only this, so a later React front end
 * can reuse the same rules.
 */
@Service
@Transactional(readOnly = true)
public class PolicySearchService {

    private static final DateTimeFormatter CHECKED = DateTimeFormatter.ofPattern("yyyy년 M월 d일 H:mm");

    private final PolicyRepository policies;
    private final Clock clock;

    public PolicySearchService(PolicyRepository policies, Clock clock) {
        this.policies = policies;
        this.clock = clock;
    }

    /** Open notices that fit the condition, soonest deadline first (no deadline last). */
    public List<PolicyCard> search(SearchCondition condition) {
        LocalDate today = LocalDate.now(clock);
        List<Region> regions = condition.region() == null
                ? Arrays.asList(Region.values())
                : condition.region().withParents();
        return policies.findByStatusAndRegionInOrderByApplyEndAscIdAsc(PolicyStatus.ACTIVE, regions).stream()
                .filter(p -> PolicyMatcher.isOpen(p, today))
                .filter(p -> PolicyMatcher.matches(p, condition))
                .map(p -> new PolicyCard(p.getId(), p.getRegion(), p.getTitle(), p.getSummary(), p.getBenefit(),
                        ApplyPeriodText.deadline(p.getApplyEnd(), p.getApplyEndTime(), today),
                        PolicyMatcher.checkNotes(p, condition)))
                .toList();
    }

    public Optional<PolicyDetail> detail(long id) {
        LocalDate today = LocalDate.now(clock);
        return policies.findById(id).map(p -> new PolicyDetail(
                p.getId(), p.getRegion(), p.getTitle(), p.getBenefit(), target(p), p.getSummary(),
                ApplyPeriodText.period(p.getApplyStart(), p.getApplyEnd(), p.getApplyEndTime()),
                ApplyPeriodText.deadline(p.getApplyEnd(), p.getApplyEndTime(), today),
                p.getApplyUrl(), p.getOfficialUrl(),
                PolicyMatcher.checkNotes(p, SearchCondition.none()),
                p.getLastCheckedAt() == null ? null : p.getLastCheckedAt().atZone(clock.getZone()).format(CHECKED),
                PolicyMatcher.isOpen(p, today)));
    }

    private static List<String> target(Policy p) {
        List<String> lines = new ArrayList<>();
        Integer min = p.getMinAge();
        Integer max = p.getMaxAge();
        if (min != null && max != null) {
            lines.add("만 " + min + "~" + max + "세");
        } else if (min != null) {
            lines.add("만 " + min + "세 이상");
        } else if (max != null) {
            lines.add("만 " + max + "세 이하");
        }
        if (p.isSmallBusinessOnly()) {
            lines.add("중소기업 재직자");
        } else if (p.isEmployedOnly()) {
            lines.add("재직자");
        }
        if (lines.isEmpty()) {
            lines.add("공식 공고에서 확인해 주세요.");
        }
        return lines;
    }
}
