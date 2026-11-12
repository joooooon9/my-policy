package com.mypolicy.policy;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Screen-check sample notices, until the official API is connected. Inserted once, when the database has
 * none (my-policy.sample-data=true). These are not official notices and have no links.
 */
@Component
@ConditionalOnProperty(name = "my-policy.sample-data", havingValue = "true")
public class SamplePolicyLoader implements ApplicationRunner {

    static final String SOURCE = "sample";
    private static final Logger log = LoggerFactory.getLogger(SamplePolicyLoader.class);

    private final PolicyRepository policies;

    public SamplePolicyLoader(PolicyRepository policies) {
        this.policies = policies;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (policies.countBySource(SOURCE) > 0) {
            return;
        }
        policies.saveAll(List.of(
                new Policy(SOURCE, "1", Region.NATIONWIDE, "청년 주거비 지원", "월세 부담을 덜어주는 청년 주거 지원")
                        .withBenefit("월 최대 20만 원")
                        .withAgeRange(19, 34)
                        .withApplyPeriod(null, LocalDate.of(2026, 11, 15), null)
                        .withCheckNotes(List.of("소득 조건 확인 필요")),
                new Policy(SOURCE, "2", Region.JEONBUK, "재직자 직무교육 지원", "직무 역량을 높이는 교육 과정 지원")
                        .withBenefit("수강료 지원")
                        .withAgeRange(19, 64)
                        .withEmployment(true, false)
                        .withApplyPeriod(null, LocalDate.of(2026, 11, 19), null),
                new Policy(SOURCE, "3", Region.IKSAN, "청년 중소기업 근속 지원", "지역 중소기업에 재직 중인 청년 지원")
                        .withBenefit("최대 100만 원")
                        .withAgeRange(19, 39)
                        .withEmployment(true, true)
                        .withApplyPeriod(null, LocalDate.of(2026, 11, 24), null)
                        .withCheckNotes(List.of("근속기간 확인 필요"))));
        log.info("Inserted 3 sample policies (source '{}'); set my-policy.sample-data=false to skip.", SOURCE);
    }
}
