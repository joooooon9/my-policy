package com.mypolicy.policy;

import java.util.List;

/**
 * One notice's detail page, ready to show. applyUrl leads to "공식 신청 페이지로 이동"; without it
 * officialUrl leads to "공식 공고 보기"; lastChecked is empty until the notice has been collected.
 * explanation is the short purpose line; support is what the policy gives.
 */
public record PolicyDetail(long id, Region region, String institution, String title, String explanation,
        String benefit, List<String> target, String support, String period, String deadline, String applyUrl,
        String officialUrl, List<String> checkNotes, String lastChecked, boolean open) {
}
