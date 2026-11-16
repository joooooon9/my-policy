package com.mypolicy.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class PolicyMatcherTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    private static Policy policy(Region region) {
        return new Policy("test", region.name(), region, "공고", "요약");
    }

    private static SearchCondition condition(String region, Integer age, String employment, String smallBusiness) {
        return SearchCondition.fromParams(region, age, employment, smallBusiness);
    }

    private static SearchCondition condition(String region, Integer age, String employment, String smallBusiness,
            String keyword) {
        return SearchCondition.fromParams(region, age, employment, smallBusiness, keyword);
    }

    @Test
    void iksanShowsIksanJeonbukAndNationwide() {
        SearchCondition iksan = condition("익산", null, "", "");
        assertThat(PolicyMatcher.matches(policy(Region.IKSAN), iksan)).isTrue();
        assertThat(PolicyMatcher.matches(policy(Region.JEONBUK), iksan)).isTrue();
        assertThat(PolicyMatcher.matches(policy(Region.NATIONWIDE), iksan)).isTrue();
    }

    @Test
    void jeonbukDoesNotShowIksanOnlyNotices() {
        SearchCondition jeonbuk = condition("전북", null, "", "");
        assertThat(PolicyMatcher.matches(policy(Region.IKSAN), jeonbuk)).isFalse();
        assertThat(PolicyMatcher.matches(policy(Region.JEONBUK), jeonbuk)).isTrue();
        assertThat(PolicyMatcher.matches(policy(Region.NATIONWIDE), jeonbuk)).isTrue();
    }

    @Test
    void ageOutsideTheRangeIsExcludedAndTheEdgesAreIncluded() {
        Policy youth = policy(Region.NATIONWIDE).withAgeRange(19, 34);
        assertThat(PolicyMatcher.matches(youth, condition("", 18, "", ""))).isFalse();
        assertThat(PolicyMatcher.matches(youth, condition("", 19, "", ""))).isTrue();
        assertThat(PolicyMatcher.matches(youth, condition("", 34, "", ""))).isTrue();
        assertThat(PolicyMatcher.matches(youth, condition("", 35, "", ""))).isFalse();
        assertThat(PolicyMatcher.matches(youth, condition("", null, "", ""))).isTrue();
    }

    @Test
    void employedOnlyNoticesAreExcludedForPeopleNotEmployed() {
        Policy workers = policy(Region.JEONBUK).withEmployment(true, false);
        assertThat(PolicyMatcher.matches(workers, condition("", null, "unemployed", ""))).isFalse();
        assertThat(PolicyMatcher.matches(workers, condition("", null, "employed", ""))).isTrue();
        assertThat(PolicyMatcher.matches(workers, condition("", null, "", ""))).isTrue();
    }

    @Test
    void unemployedOnlyNoticesAreExcludedForEmployedPeople() {
        Policy jobSeekers = policy(Region.NATIONWIDE).withUnemployedOnly(true);
        assertThat(PolicyMatcher.matches(jobSeekers, condition("", null, "employed", ""))).isFalse();
        assertThat(PolicyMatcher.matches(jobSeekers, condition("", null, "unemployed", ""))).isTrue();
        assertThat(PolicyMatcher.matches(jobSeekers, condition("", null, "", ""))).isTrue();
    }

    @Test
    void closedNoticesAreNotOpen() {
        Policy closed = policy(Region.NATIONWIDE);
        closed.close();
        assertThat(PolicyMatcher.isOpen(closed, TODAY)).isFalse();
        closed.reopen();
        assertThat(PolicyMatcher.isOpen(closed, TODAY)).isTrue();
    }

    @Test
    void smallBusinessNoExcludesButUnknownOnlyAddsANote() {
        Policy smallBusiness = policy(Region.IKSAN).withEmployment(false, true)
                .withCheckNotes(List.of("근속기간 확인 필요"));

        assertThat(PolicyMatcher.matches(smallBusiness, condition("", null, "employed", "no"))).isFalse();

        SearchCondition unknown = condition("", null, "employed", "");
        assertThat(PolicyMatcher.matches(smallBusiness, unknown)).isTrue();
        assertThat(PolicyMatcher.checkNotes(smallBusiness, unknown))
                .containsExactly("근속기간 확인 필요", "중소기업 재직 여부 확인 필요");

        SearchCondition yes = condition("", null, "employed", "yes");
        assertThat(PolicyMatcher.checkNotes(smallBusiness, yes)).containsExactly("근속기간 확인 필요");
    }

    @Test
    void theSmallBusinessAnswerCountsOnlyForEmployedPeople() {
        Policy smallBusiness = policy(Region.IKSAN).withEmployment(false, true);
        SearchCondition notEmployedButAnsweredNo = condition("", null, "", "no");
        assertThat(notEmployedButAnsweredNo.smallBusiness()).isEqualTo(SearchCondition.SmallBusiness.UNKNOWN);
        assertThat(PolicyMatcher.matches(smallBusiness, notEmployedButAnsweredNo)).isTrue();
    }

    @Test
    void openFollowsTheKoreanDateAndCancellation() {
        Policy endsToday = policy(Region.NATIONWIDE).withApplyPeriod(null, TODAY, null);
        assertThat(PolicyMatcher.isOpen(endsToday, TODAY)).isTrue();
        assertThat(PolicyMatcher.isOpen(endsToday, TODAY.plusDays(1))).isFalse();

        Policy notStarted = policy(Region.NATIONWIDE).withApplyPeriod(TODAY.plusDays(1), null, null);
        assertThat(PolicyMatcher.isOpen(notStarted, TODAY)).isFalse();

        Policy noDates = policy(Region.NATIONWIDE);
        assertThat(PolicyMatcher.isOpen(noDates, TODAY)).isTrue();
        noDates.cancel();
        assertThat(PolicyMatcher.isOpen(noDates, TODAY)).isFalse();
    }

    @Test
    void everyKeywordWordMustAppearInTheNoticeWordsInAnyCase() {
        Policy rent = new Policy("test", "rent", Region.IKSAN, "청년 월세 지원", "월세 부담을 덜어 드려요")
                .withSupportContent("월 최대 20만 원, AI 교육 포함").withInstitution("전북특별자치도 익산시");

        assertThat(PolicyMatcher.matches(rent, condition("", null, "", "", "월세"))).isTrue();
        assertThat(PolicyMatcher.matches(rent, condition("", null, "", "", "익산시 월세"))).isTrue(); // institution + title
        assertThat(PolicyMatcher.matches(rent, condition("", null, "", "", "ai"))).isTrue(); // support text, any case
        assertThat(PolicyMatcher.matches(rent, condition("", null, "", "", "월세 창업"))).isFalse(); // one word missing
        assertThat(PolicyMatcher.matches(rent, condition("", null, "", "", "   "))).isTrue(); // blank filters nothing
    }

    @Test
    void aKeywordAloneIsASearch() {
        assertThat(condition("", null, "", "", "  월세  ").keyword()).isEqualTo("월세");
        assertThat(condition("", null, "", "", "월세").isSearch()).isTrue();
        assertThat(condition("", null, "", "", " ").isSearch()).isFalse();
    }

    @Test
    void anEmptyFormIsNotASearch() {
        assertThat(condition("", null, "", "").isSearch()).isFalse();
        assertThat(condition("익산", null, "", "").isSearch()).isTrue();
        assertThat(condition("", 28, "", "").isSearch()).isTrue();
        assertThat(condition("", null, "employed", "").isSearch()).isTrue();
        assertThat(condition("서울", null, "", "").region()).isNull();
    }
}
