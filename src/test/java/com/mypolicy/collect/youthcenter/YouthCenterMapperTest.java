package com.mypolicy.collect.youthcenter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mypolicy.collect.SourcePolicy;
import com.mypolicy.policy.Region;

/** Items shaped like the live getPlcy response (2026-11-16): every value a string, blanks as a single space. */
class YouthCenterMapperTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private static final String DETAIL = "https://www.youthcenter.go.kr/youthPolicy/ythPlcyTotalSearch/ythPlcyDetail/";
    private static final ObjectMapper JSON = new ObjectMapper();

    /** 256 district codes, 익산 (52140) among them. */
    private static final String ALL_DISTRICTS = IntStream.range(0, 255).mapToObj(i -> String.valueOf(11000 + i))
            .collect(Collectors.joining(",")) + ",52140";

    private static Map<String, String> base() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("plcyNo", "20261104005400214721");
        m.put("plcyNm", "청년 주거 지원");
        m.put("plcyExplnCn", "청년의 주거 안정을 돕습니다.");
        m.put("plcySprtCn", "월 최대 20만 원\n최대 12개월");
        m.put("operInstCdNm", " ");
        m.put("sprvsnInstCdNm", "국토교통부");
        m.put("rgtrInstCdNm", "국토교통부");
        m.put("aplyPrdSeCd", "0057001");
        m.put("aplyYmd", "20261102 ~ 20261211");
        m.put("aplyUrlAddr", "https://apply.example.go.kr/a");
        m.put("refUrlAddr1", "https://www.example.go.kr/notice");
        m.put("refUrlAddr2", " ");
        m.put("sprtTrgtMinAge", "19");
        m.put("sprtTrgtMaxAge", "34");
        m.put("sprtTrgtAgeLmtYn", "N");
        m.put("earnCndSeCd", "0043001");
        m.put("mrgSttsCd", "0055003");
        m.put("jobCd", "0013010");
        m.put("sbizCd", "0014010");
        m.put("schoolCd", "0049010");
        m.put("plcyMajorCd", "0011009");
        m.put("addAplyQlfcCndCn", " ");
        m.put("ptcpPrpTrgtCn", " ");
        m.put("zipCd", ALL_DISTRICTS);
        return m;
    }

    private static SourcePolicy map(Map<String, String> fields) {
        JsonNode node = JSON.valueToTree(fields);
        return YouthCenterMapper.map(node, TODAY, DETAIL).orElseThrow();
    }

    private static Map<String, String> with(String... pairs) {
        Map<String, String> m = base();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }

    @Test
    void aNationwidePolicyMapsWithItsPeriodLinksAndInstitution() {
        SourcePolicy p = map(base());

        assertThat(p.sourceId()).isEqualTo("20261104005400214721");
        assertThat(p.region()).isEqualTo(Region.NATIONWIDE);
        assertThat(p.institution()).isEqualTo("국토교통부"); // operInstCdNm was blank
        assertThat(p.summary()).isEqualTo("청년의 주거 안정을 돕습니다.");
        assertThat(p.supportContent()).isEqualTo("월 최대 20만 원\n최대 12개월");
        assertThat(p.minAge()).isEqualTo(19);
        assertThat(p.maxAge()).isEqualTo(34);
        assertThat(p.applyStart()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(p.applyEnd()).isEqualTo(LocalDate.of(2026, 12, 11));
        assertThat(p.applyEndTime()).isNull();
        assertThat(p.applyUrl()).isEqualTo("https://apply.example.go.kr/a");
        assertThat(p.officialUrl()).isEqualTo("https://www.example.go.kr/notice");
        assertThat(p.checkNotes()).isEmpty();
        assertThat(p.closed()).isFalse();
        assertThat(p.cancelled()).isFalse();
    }

    @Test
    void theRegionFollowsWhetherIksanResidentsMayApply() {
        assertThat(YouthCenterMapper.regionOf(ALL_DISTRICTS)).contains(Region.NATIONWIDE);
        assertThat(YouthCenterMapper.regionOf("52140")).contains(Region.IKSAN);
        assertThat(YouthCenterMapper.regionOf("52111,52113,52130,52140,52180")).contains(Region.JEONBUK);
        assertThat(YouthCenterMapper.regionOf("52111,52113")).isEmpty(); // 전주 only
        assertThat(YouthCenterMapper.regionOf("11110,11140")).isEmpty(); // 서울 only
        assertThat(YouthCenterMapper.map(JSON.valueToTree(with("zipCd", "11110")), TODAY, DETAIL)).isEmpty();
    }

    @Test
    void alwaysOpenHasNoDatesAndClosedIsMarked() {
        SourcePolicy always = map(with("aplyPrdSeCd", "0057002", "aplyYmd", " "));
        assertThat(always.applyStart()).isNull();
        assertThat(always.applyEnd()).isNull();
        assertThat(always.closed()).isFalse();

        SourcePolicy closed = map(with("aplyPrdSeCd", "0057003", "aplyYmd", " "));
        assertThat(closed.closed()).isTrue();
    }

    @Test
    void severalPeriodsPickTheOneRunningOrNext() {
        String two = "20260101 ~ 20261231\\N20270101 ~ 20271231";
        assertThat(YouthCenterMapper.period(two, TODAY)).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertThat(YouthCenterMapper.period(two, LocalDate.of(2027, 3, 1))).containsExactly(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31));
        String later = "20261001 ~ 20261010\\N20270201 ~ 20270210";
        assertThat(YouthCenterMapper.period(later, TODAY)).containsExactly(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 10));
    }

    @Test
    void anEndBeforeItsStartAndBrokenPeriodsBecomeUnknown() {
        assertThat(YouthCenterMapper.period("20260309 ~ 20250316", TODAY)).containsExactly(LocalDate.of(2026, 3, 9), null);
        assertThat(YouthCenterMapper.period("~", TODAY)).containsExactly(null, null);
        assertThat(YouthCenterMapper.period("00010101 ~ 00010101", TODAY)).containsExactly(null, null);
        assertThat(YouthCenterMapper.period("20260101 ~ 00021231", TODAY)).containsExactly(LocalDate.of(2026, 1, 1), null);
        assertThat(YouthCenterMapper.period(null, TODAY)).containsExactly(null, null);
    }

    @Test
    void employmentAndSmallBusinessCodesSetTheFlags() {
        SourcePolicy employed = map(with("jobCd", "0013001"));
        assertThat(employed.employedOnly()).isTrue();
        assertThat(employed.unemployedOnly()).isFalse();

        SourcePolicy jobSeekers = map(with("jobCd", "0013003"));
        assertThat(jobSeekers.unemployedOnly()).isTrue();

        SourcePolicy mixed = map(with("jobCd", "0013001,0013003"));
        assertThat(mixed.employedOnly()).isFalse();
        assertThat(mixed.unemployedOnly()).isFalse();
        assertThat(mixed.checkNotes()).contains("취업 상태 조건 확인 필요");

        SourcePolicy smallBusiness = map(with("sbizCd", "0014001"));
        assertThat(smallBusiness.smallBusinessOnly()).isTrue();
        assertThat(smallBusiness.employedOnly()).isTrue();

        SourcePolicy disabled = map(with("sbizCd", "0014005"));
        assertThat(disabled.smallBusinessOnly()).isFalse();
        assertThat(disabled.checkNotes()).contains("특화 대상 조건 확인 필요");
    }

    @Test
    void conditionsTheFormCannotDecideBecomeNotes() {
        SourcePolicy p = map(with("earnCndSeCd", "0043003", "mrgSttsCd", "0055001", "schoolCd", "0049005",
                "plcyMajorCd", "0011004", "addAplyQlfcCndCn", "기초생활수급자", "ptcpPrpTrgtCn", "기존 수혜자"));
        assertThat(p.checkNotes()).containsExactly("소득 조건 확인 필요", "혼인 여부 조건 확인 필요", "학력 조건 확인 필요",
                "전공 조건 확인 필요", "추가 자격 조건 확인 필요", "참여 제한 대상 확인 필요");
    }

    @Test
    void zeroOrReversedAgesAreNotUsedToExclude() {
        SourcePolicy none = map(with("sprtTrgtMinAge", "0", "sprtTrgtMaxAge", "0"));
        assertThat(none.minAge()).isNull();
        assertThat(none.maxAge()).isNull();

        SourcePolicy reversed = map(with("sprtTrgtMinAge", "39", "sprtTrgtMaxAge", "19"));
        assertThat(reversed.minAge()).isNull();
        assertThat(reversed.checkNotes()).contains("연령 조건 확인 필요");
    }

    @Test
    void withoutItsOwnLinkThePolicyPointsToItsYouthCenterPage() {
        SourcePolicy p = map(with("aplyUrlAddr", " ", "refUrlAddr1", "-", "refUrlAddr2", " "));
        assertThat(p.applyUrl()).isNull();
        assertThat(p.officialUrl()).isEqualTo(DETAIL + "20261104005400214721");

        SourcePolicy bare = map(with("refUrlAddr1", "www.example.go.kr/notice", "aplyUrlAddr", "javascript:void(0)"));
        assertThat(bare.officialUrl()).isEqualTo("https://www.example.go.kr/notice");
        assertThat(bare.applyUrl()).isNull();
    }
}
