package com.mypolicy.policy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Words for the application period, counted in Korean dates. Only what the notice states is shown:
 * a closing time appears only when the notice gives one, and no date is guessed when it gives none.
 */
public final class ApplyPeriodText {

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("M월 d일");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("yyyy년 M월 d일");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("H:mm");

    private ApplyPeriodText() {
    }

    /** For a card: "11월 15일 마감 · D-3", "11월 15일 18:00 마감 · D-DAY", or a pointer to the notice. */
    public static String deadline(LocalDate end, LocalTime endTime, LocalDate today) {
        if (end == null) {
            return "마감일은 공식 공고에서 확인";
        }
        long days = ChronoUnit.DAYS.between(today, end);
        String dDay = days == 0 ? "D-DAY" : days > 0 ? "D-" + days : "마감됨";
        return end.format(SHORT) + time(endTime) + " 마감 · " + dDay;
    }

    /** For the detail page: "2026년 11월 1일 ~ 2026년 11월 15일", one side when only one is given. */
    public static String period(LocalDate start, LocalDate end, LocalTime endTime) {
        if (start == null && end == null) {
            return "신청 기간은 공식 공고에서 확인해 주세요.";
        }
        String from = start == null ? "" : start.format(FULL);
        String to = end == null ? "" : end.format(FULL) + time(endTime);
        if (start == null) {
            return to + "까지";
        }
        if (end == null) {
            return from + "부터";
        }
        return from + " ~ " + to;
    }

    private static String time(LocalTime endTime) {
        return endTime == null ? "" : " " + endTime.format(TIME);
    }
}
