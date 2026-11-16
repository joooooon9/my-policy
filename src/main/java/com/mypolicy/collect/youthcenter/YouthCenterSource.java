package com.mypolicy.collect.youthcenter;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.mypolicy.collect.PolicySource;
import com.mypolicy.collect.SourcePolicy;
import com.mypolicy.policy.Region;

/**
 * 온통청년 청년정책 API (getPlcy). Reads every page, keeps the policies 익산 residents can apply to in the
 * regions collected so far (my-policy.youthcenter.regions). Exists only when an API key is set.
 */
@Component
@EnableConfigurationProperties(YouthCenterProperties.class)
@ConditionalOnExpression("!'${my-policy.youthcenter.api-key:}'.isBlank()")
public class YouthCenterSource implements PolicySource {

    private static final Logger log = LoggerFactory.getLogger(YouthCenterSource.class);

    private final YouthCenterProperties props;
    private final RestClient http;
    private final Clock clock;

    public YouthCenterSource(YouthCenterProperties props, RestClient.Builder http, Clock clock) {
        this.props = props;
        this.http = http.build();
        this.clock = clock;
    }

    @Override
    public String name() {
        return YouthCenterMapper.SOURCE;
    }

    @Override
    public Set<Region> regions() {
        return props.regions();
    }

    @Override
    public List<SourcePolicy> fetch() throws Exception {
        LocalDate today = LocalDate.now(clock);
        List<SourcePolicy> kept = new ArrayList<>();
        int read = 0;
        Integer total = null;
        for (int page = 1; page <= props.maxPages(); page++) {
            JsonNode body = page(page);
            JsonNode list = body.path("result").path("youthPolicyList");
            if (total == null) {
                total = body.path("result").path("pagging").path("totCount").asInt(-1);
            }
            if (!list.isArray() || list.isEmpty()) {
                break;
            }
            for (JsonNode item : list) {
                read++;
                YouthCenterMapper.map(item, today, props.detailUrl())
                        .filter(p -> props.regions().contains(p.region()))
                        .ifPresent(kept::add);
            }
            if (total >= 0 && read >= total) {
                break;
            }
            Thread.sleep(props.pageDelay().toMillis());
        }
        if (total != null && total >= 0 && read < total) {
            // a partial listing would make missing notices look gone: fail the run instead
            throw new IllegalStateException("온통청년 listed " + total + " policies but only " + read + " were read");
        }
        log.info("온통청년: read {} policies, kept {} for regions {}", read, kept.size(), props.regions());
        return kept;
    }

    /** One page, asked again after a pause when it fails (the API refuses an occasional page mid-run). */
    private JsonNode page(int page) throws InterruptedException {
        String last = null;
        for (int attempt = 0; attempt <= props.retries(); attempt++) {
            if (attempt > 0) {
                log.info("온통청년 page {} failed ({}); asking again ({} of {})", page, last, attempt, props.retries());
                Thread.sleep(props.retryBackoff().toMillis() * attempt);
            }
            try {
                JsonNode body = http.get().uri(uri(page)).accept(MediaType.APPLICATION_JSON).retrieve().body(JsonNode.class);
                if (body != null && body.path("resultCode").asInt() == 200) {
                    return body;
                }
                last = body == null ? "empty response" : "resultCode " + body.path("resultCode").asText() + " " + body.path("resultMessage").asText();
            } catch (RestClientResponseException e) {
                last = e.getStatusCode().value() + " " + e.getResponseBodyAsString();
            } catch (RestClientException e) {
                // the message of an I/O error repeats the request URI, which carries the API key: keep only the type
                last = e.getClass().getSimpleName();
            }
        }
        throw new IllegalStateException("온통청년 page " + page + " failed after " + (props.retries() + 1) + " tries: " + last);
    }

    private URI uri(int page) {
        return UriComponentsBuilder.fromUriString(props.baseUrl())
                .queryParam("apiKeyNm", props.apiKey())
                .queryParam("pageNum", page)
                .queryParam("pageSize", props.pageSize())
                .queryParam("rtnType", "json")
                .encode()
                .build()
                .toUri();
    }
}
