package com.mypolicy.collect.youthcenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mypolicy.collect.SourcePolicy;
import com.mypolicy.policy.Region;

/** Paging and retries of the 온통청년 source against a mock server (no network). */
class YouthCenterSourceTest {

    private static final String KEY = "SECRET-KEY-123";
    private static final String ALL = IntStream.range(0, 255).mapToObj(i -> String.valueOf(11000 + i))
            .collect(Collectors.joining(",")) + ",52140";
    private static final String OK = """
            {"resultCode":200,"resultMessage":"성공","result":{"pagging":{"totCount":1,"pageNum":1,"pageSize":100},
             "youthPolicyList":[{"plcyNo":"P1","plcyNm":"청년 주거 지원","plcyExplnCn":"설명","zipCd":"%s",
             "aplyPrdSeCd":"0057002","aplyYmd":" ","jobCd":"0013010","sbizCd":"0014010"}]}}
            """.formatted(ALL);
    private static final String REFUSED = "{\"errorCode\":\"e001\",\"errorMsg\":\"invalid api key.\"}";

    private MockRestServiceServer server;
    private YouthCenterSource source;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        YouthCenterProperties props = new YouthCenterProperties(KEY, "https://yc.example/getPlcy",
                "https://yc.example/detail/", 100, 10, Duration.ZERO, 2, Duration.ZERO, Set.of(Region.NATIONWIDE));
        source = new YouthCenterSource(props, builder,
                Clock.fixed(Instant.parse("2026-11-17T01:00:00Z"), ZoneId.of("Asia/Seoul")));
    }

    @Test
    void aRefusedPageIsAskedAgainAndTheRunGoesOn() throws Exception {
        server.expect(ExpectedCount.once(), method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body(REFUSED));
        server.expect(ExpectedCount.once(), method(HttpMethod.GET))
                .andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));

        List<SourcePolicy> listing = source.fetch();

        assertThat(listing).extracting(SourcePolicy::sourceId).containsExactly("P1");
        server.verify();
    }

    @Test
    void aPageRefusedEveryTimeFailsTheRunNamingThePageButNeverTheKey() {
        server.expect(ExpectedCount.times(3), method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body(REFUSED));

        assertThatThrownBy(() -> source.fetch())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("page 1")
                .hasMessageContaining("3 tries")
                .hasMessageContaining("403")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(KEY));
        server.verify();
    }

    @Test
    void anErrorResultInAnOkResponseIsRetriedToo() throws Exception {
        server.expect(ExpectedCount.once(), method(HttpMethod.GET))
                .andRespond(withSuccess("{\"resultCode\":500,\"resultMessage\":\"잠시 후 다시\"}", MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), method(HttpMethod.GET))
                .andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));

        assertThat(source.fetch()).hasSize(1);
        server.verify();
    }
}
