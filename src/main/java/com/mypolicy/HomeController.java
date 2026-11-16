package com.mypolicy;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

import com.mypolicy.collect.CollectedRegions;
import com.mypolicy.policy.PolicyCard;
import com.mypolicy.policy.PolicySearchService;
import com.mypolicy.policy.SearchCondition;

@Controller
public class HomeController {

    /** Notices shown at first and added by each "더 보기": 3 columns x 4 rows on a desktop. */
    static final int PAGE_SIZE = 12;

    private final PolicySearchService search;
    private final CollectedRegions collectedRegions;

    public HomeController(PolicySearchService search, CollectedRegions collectedRegions) {
        this.search = search;
        this.collectedRegions = collectedRegions;
    }

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "") String region,
            @RequestParam(required = false) Integer age,
            @RequestParam(defaultValue = "") String employment,
            @RequestParam(defaultValue = "") String smallBusiness,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "1") int page, Model model) {
        SearchCondition condition = SearchCondition.fromParams(region, age, employment, smallBusiness, q);
        List<PolicyCard> all = search.search(condition);
        // page N shows the first N x 12 (so a reload or a back from a detail page keeps what was opened)
        int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int shownPages = Math.min(Math.max(page, 1), pages);
        int shown = Math.min(all.size(), shownPages * PAGE_SIZE);
        model.addAttribute("policies", all.subList(0, shown));
        model.addAttribute("total", all.size());
        model.addAttribute("page", shownPages);
        model.addAttribute("pageSize", PAGE_SIZE);
        model.addAttribute("hasMore", shown < all.size());
        model.addAttribute("moreUrl", moreUrl(q, region, age, employment, smallBusiness, shownPages + 1));
        model.addAttribute("searched", condition.isSearch());
        model.addAttribute("regions", collectedRegions.list());
        model.addAttribute("region", region);
        model.addAttribute("age", age);
        model.addAttribute("employment", employment);
        model.addAttribute("smallBusiness", smallBusiness);
        model.addAttribute("q", condition.keyword() == null ? "" : condition.keyword());
        return "index";
    }

    /** The next page with only the conditions that were filled in, jumping to its first card without script. */
    private static String moreUrl(String q, String region, Integer age, String employment, String smallBusiness, int page) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath("/");
        if (!q.isBlank()) url.queryParam("q", q.strip());
        if (!region.isBlank()) url.queryParam("region", region);
        if (age != null) url.queryParam("age", age);
        if (!employment.isBlank()) url.queryParam("employment", employment);
        if (!smallBusiness.isBlank()) url.queryParam("smallBusiness", smallBusiness);
        return url.queryParam("page", page).fragment("p" + page).encode().build().toUriString();
    }

    @GetMapping("/policies/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("policy", search.detail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "policy-detail";
    }
}
