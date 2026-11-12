package com.mypolicy.policy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.mypolicy.policy.SearchCondition.Employment;
import com.mypolicy.policy.SearchCondition.SmallBusiness;

/**
 * The matching rules. A notice is left out only when an entered condition clearly contradicts a required one;
 * conditions the form cannot decide (income, tenure, an unanswered small-business question) are returned as
 * notes to show beside the notice instead.
 */
public final class PolicyMatcher {

    static final String SMALL_BUSINESS_NOTE = "중소기업 재직 여부 확인 필요";

    private PolicyMatcher() {
    }

    /** Still open on the given day (Korean date): not cancelled, started, and not past its last day. */
    public static boolean isOpen(Policy policy, LocalDate today) {
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            return false;
        }
        if (policy.getApplyStart() != null && today.isBefore(policy.getApplyStart())) {
            return false;
        }
        return policy.getApplyEnd() == null || !today.isAfter(policy.getApplyEnd());
    }

    public static boolean matches(Policy policy, SearchCondition condition) {
        if (condition.region() != null && !condition.region().withParents().contains(policy.getRegion())) {
            return false;
        }
        Integer age = condition.age();
        if (age != null) {
            if (policy.getMinAge() != null && age < policy.getMinAge()) {
                return false;
            }
            if (policy.getMaxAge() != null && age > policy.getMaxAge()) {
                return false;
            }
        }
        if (policy.isEmployedOnly() && condition.employment() == Employment.UNEMPLOYED) {
            return false;
        }
        return !(policy.isSmallBusinessOnly() && condition.smallBusiness() == SmallBusiness.NO);
    }

    /** The notice's own notes, plus the small-business check when that answer is still open. */
    public static List<String> checkNotes(Policy policy, SearchCondition condition) {
        List<String> notes = new ArrayList<>(policy.getCheckNotes());
        if (policy.isSmallBusinessOnly() && condition.smallBusiness() != SmallBusiness.YES
                && !notes.contains(SMALL_BUSINESS_NOTE)) {
            notes.add(SMALL_BUSINESS_NOTE);
        }
        return notes;
    }
}
