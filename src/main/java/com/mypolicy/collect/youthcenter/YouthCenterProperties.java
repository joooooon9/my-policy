package com.mypolicy.collect.youthcenter;

import java.time.Duration;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import com.mypolicy.policy.Region;

/**
 * my-policy.youthcenter.*: the 온통청년 youth policy API.
 * api-key comes from YOUTHCENTER_API_KEY (.env or the environment); without it the source is not created.
 * regions are the service regions collected so far (CLAUDE.md: nationwide first, then 전북 and 익산).
 */
@ConfigurationProperties("my-policy.youthcenter")
public record YouthCenterProperties(
        String apiKey,
        @DefaultValue("https://www.youthcenter.go.kr/go/ythip/getPlcy") String baseUrl,
        @DefaultValue("https://www.youthcenter.go.kr/youthPolicy/ythPlcyTotalSearch/ythPlcyDetail/") String detailUrl,
        @DefaultValue("100") int pageSize,
        @DefaultValue("100") int maxPages,
        @DefaultValue("1s") Duration pageDelay,
        /** A page that fails is asked again this many times more, waiting retryBackoff, then twice that, ... */
        @DefaultValue("2") int retries,
        @DefaultValue("3s") Duration retryBackoff,
        @DefaultValue("NATIONWIDE") Set<Region> regions) {
}
