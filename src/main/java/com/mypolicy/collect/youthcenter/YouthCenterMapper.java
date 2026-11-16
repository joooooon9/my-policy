package com.mypolicy.collect.youthcenter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.mypolicy.collect.SourcePolicy;
import com.mypolicy.policy.Region;

/**
 * Turns one item of 온통청년 getPlcy (result.youthPolicyList[]) into a SourcePolicy. Pure, no I/O.
 *
 * <p>The meanings below were read from the live API on 2026-11-16 (the public code table did not load);
 * docs/DEV_NOTES.md lists them as inferred until checked against the official 코드정의서.
 * <ul>
 * <li>zipCd: the 시군구 codes whose residents may apply. 익산시 is 52140 (전북특별자치도 52xxx).</li>
 * <li>aplyPrdSeCd: 0057001 specific period (aplyYmd "YYYYMMDD ~ YYYYMMDD", several joined by \N),
 * 0057002 always open, 0057003 closed.</li>
 * <li>jobCd: 0013001 employed, 0013003 not in work, 0013010 no restriction.</li>
 * <li>sbizCd: 0014001 small and medium business, 0014010 no restriction.</li>
 * <li>earnCndSeCd: 0043001 no income condition; 0043002 an income range; 0043003 other income wording.</li>
 * <li>mrgSttsCd 0055003, schoolCd 0049010, plcyMajorCd 0011009: no restriction.</li>
 * </ul>
 */
public final class YouthCenterMapper {

    static final String SOURCE = "youthcenter";
    static final String IKSAN = "52140";
    static final String JEONBUK_PREFIX = "52";
    /** A policy open to residents of at least this many 시군구 (of about 256) counts as nationwide. */
    static final int NATIONWIDE_MIN_DISTRICTS = 200;

    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    /** Years before this are placeholders (the API sends 00010101 for some policies). */
    static final int EARLIEST_YEAR = 2000;
    private static final Pattern PERIOD = Pattern.compile("(\\d{8})\\s*~\\s*(\\d{8})");

    private YouthCenterMapper() {
    }

    /** The service region a policy belongs to for 익산 residents, or empty when 익산 residents cannot apply. */
    public static Optional<Region> regionOf(String zipCd) {
        List<String> codes = codes(zipCd);
        if (!codes.contains(IKSAN)) {
            return Optional.empty();
        }
        if (codes.size() == 1) {
            return Optional.of(Region.IKSAN);
        }
        if (codes.size() >= NATIONWIDE_MIN_DISTRICTS) {
            return Optional.of(Region.NATIONWIDE);
        }
        return Optional.of(Region.JEONBUK);
    }

    public static Optional<SourcePolicy> map(JsonNode item, LocalDate today, String detailUrl) {
        String id = text(item, "plcyNo");
        String title = text(item, "plcyNm");
        Optional<Region> region = regionOf(raw(item, "zipCd"));
        if (id == null || title == null || region.isEmpty()) {
            return Optional.empty();
        }

        List<String> notes = new ArrayList<>();
        Integer minAge = age(item, "sprtTrgtMinAge");
        Integer maxAge = age(item, "sprtTrgtMaxAge");
        if (minAge != null && maxAge != null && minAge > maxAge) {
            minAge = null;
            maxAge = null;
            notes.add("연령 조건 확인 필요");
        }

        Set<String> job = Set.copyOf(codes(raw(item, "jobCd")));
        boolean employedOnly = job.equals(Set.of("0013001"));
        boolean unemployedOnly = job.equals(Set.of("0013003"));
        if (!job.isEmpty() && !job.contains("0013010") && !employedOnly && !unemployedOnly) {
            notes.add("취업 상태 조건 확인 필요");
        }

        Set<String> special = Set.copyOf(codes(raw(item, "sbizCd")));
        boolean smallBusinessOnly = special.equals(Set.of("0014001"));
        if (!special.isEmpty() && !special.contains("0014010") && !smallBusinessOnly) {
            notes.add("특화 대상 조건 확인 필요");
        }

        String earn = text(item, "earnCndSeCd");
        if ("0043002".equals(earn) || "0043003".equals(earn)) {
            notes.add("소득 조건 확인 필요");
        }
        String marriage = text(item, "mrgSttsCd");
        if ("0055001".equals(marriage) || "0055002".equals(marriage)) {
            notes.add("혼인 여부 조건 확인 필요");
        }
        if (restricted(raw(item, "schoolCd"), "0049010")) {
            notes.add("학력 조건 확인 필요");
        }
        if (restricted(raw(item, "plcyMajorCd"), "0011009")) {
            notes.add("전공 조건 확인 필요");
        }
        if (text(item, "addAplyQlfcCndCn") != null) {
            notes.add("추가 자격 조건 확인 필요");
        }
        if (text(item, "ptcpPrpTrgtCn") != null) {
            notes.add("참여 제한 대상 확인 필요");
        }

        String periodType = text(item, "aplyPrdSeCd");
        boolean closed = "0057003".equals(periodType);
        LocalDate[] period = "0057002".equals(periodType) ? new LocalDate[2] : period(raw(item, "aplyYmd"), today);

        String officialUrl = firstUrl(text(item, "refUrlAddr1"), text(item, "refUrlAddr2"));
        if (officialUrl == null && detailUrl != null) {
            officialUrl = detailUrl + id;
        }

        return Optional.of(new SourcePolicy(
                id,
                region.get(),
                cut(firstText(item, "operInstCdNm", "sprvsnInstCdNm", "rgtrInstCdNm"), 200),
                cut(title, 300),
                text(item, "plcyExplnCn"),
                text(item, "plcySprtCn"),
                null,
                minAge,
                maxAge,
                employedOnly || smallBusinessOnly,
                smallBusinessOnly,
                unemployedOnly,
                period[0],
                period[1],
                null, // the API gives dates only: no closing time is invented
                officialUrl,
                url(text(item, "aplyUrlAddr")),
                List.copyOf(new LinkedHashSet<>(notes)),
                closed,
                false)); // the API has no cancellation field (every item is 승인, 0044002)
    }

    /**
     * Start and end of the period that matters today: the one running now, else the next to start,
     * else the last. An end before its start is treated as unknown (seen in the data: a typo).
     */
    static LocalDate[] period(String aplyYmd, LocalDate today) {
        if (aplyYmd == null) {
            return new LocalDate[2];
        }
        List<LocalDate[]> periods = new ArrayList<>();
        for (String part : aplyYmd.split("\\\\N|\\n")) {
            Matcher m = PERIOD.matcher(part);
            if (m.find()) {
                try {
                    LocalDate start = LocalDate.parse(m.group(1), YMD);
                    LocalDate end = LocalDate.parse(m.group(2), YMD);
                    if (start.getYear() < EARLIEST_YEAR) {
                        continue; // a placeholder such as 00010101 (seen in the data): no date known
                    }
                    periods.add(new LocalDate[] { start,
                            end.isBefore(start) || end.getYear() < EARLIEST_YEAR ? null : end });
                } catch (DateTimeParseException e) {
                    // an impossible date: skip this part
                }
            }
        }
        if (periods.isEmpty()) {
            return new LocalDate[2];
        }
        for (LocalDate[] p : periods) {
            if (!today.isBefore(p[0]) && (p[1] == null || !today.isAfter(p[1]))) {
                return p;
            }
        }
        Optional<LocalDate[]> next = periods.stream().filter(p -> p[0].isAfter(today))
                .min(Comparator.comparing((LocalDate[] p) -> p[0]));
        return next.orElse(periods.get(periods.size() - 1));
    }

    static List<String> codes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static boolean restricted(String value, String noRestriction) {
        List<String> codes = codes(value);
        return !codes.isEmpty() && !codes.contains(noRestriction);
    }

    private static Integer age(JsonNode item, String field) {
        String value = text(item, field);
        if (value == null) {
            return null;
        }
        try {
            int age = Integer.parseInt(value);
            return age > 0 ? age : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstUrl(String... values) {
        for (String value : values) {
            String url = url(value);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    /** http(s) links only; a bare "www." gets https; anything else is dropped rather than guessed. */
    static String url(String value) {
        if (value == null) {
            return null;
        }
        String url = value.startsWith("www.") ? "https://" + value : value;
        if (!url.matches("(?i)https?://\\S+") || url.length() > 1000) {
            return null;
        }
        return url;
    }

    private static String firstText(JsonNode item, String... fields) {
        for (String field : fields) {
            String value = text(item, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String raw(JsonNode item, String field) {
        JsonNode node = item.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    /** Trimmed text, or null for missing, blank (the API sends a single space) and a bare "-". */
    static String text(JsonNode item, String field) {
        String value = raw(item, field);
        if (value == null) {
            return null;
        }
        value = value.strip();
        return value.isEmpty() || value.equals("-") ? null : value;
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
