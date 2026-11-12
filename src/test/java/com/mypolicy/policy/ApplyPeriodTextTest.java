package com.mypolicy.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

class ApplyPeriodTextTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    @Test
    void deadlineCountsDaysToTheLastDay() {
        assertThat(ApplyPeriodText.deadline(LocalDate.of(2026, 11, 15), null, TODAY)).isEqualTo("11월 15일 마감 · D-3");
        assertThat(ApplyPeriodText.deadline(TODAY, null, TODAY)).isEqualTo("11월 12일 마감 · D-DAY");
    }

    @Test
    void aClosingTimeAppearsOnlyWhenTheNoticeGivesOne() {
        assertThat(ApplyPeriodText.deadline(TODAY, LocalTime.of(18, 0), TODAY)).isEqualTo("11월 12일 18:00 마감 · D-DAY");
        assertThat(ApplyPeriodText.period(null, TODAY, LocalTime.of(18, 0))).isEqualTo("2026년 11월 12일 18:00까지");
    }

    @Test
    void noDatesPointToTheOfficialNotice() {
        assertThat(ApplyPeriodText.deadline(null, null, TODAY)).isEqualTo("마감일은 공식 공고에서 확인");
        assertThat(ApplyPeriodText.period(null, null, null)).isEqualTo("신청 기간은 공식 공고에서 확인해 주세요.");
    }

    @Test
    void periodShowsWhicheverEndsTheNoticeGives() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 15);
        assertThat(ApplyPeriodText.period(start, end, null)).isEqualTo("2026년 11월 1일 ~ 2026년 11월 15일");
        assertThat(ApplyPeriodText.period(start, null, null)).isEqualTo("2026년 11월 1일부터");
    }
}
