package com.mypolicy;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.mypolicy.policy.PolicySearchService;
import com.mypolicy.policy.Region;
import com.mypolicy.policy.SearchCondition;

@Controller
public class HomeController {

    private final PolicySearchService search;

    public HomeController(PolicySearchService search) {
        this.search = search;
    }

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "") String region,
            @RequestParam(required = false) Integer age,
            @RequestParam(defaultValue = "") String employment,
            @RequestParam(defaultValue = "") String smallBusiness, Model model) {
        SearchCondition condition = SearchCondition.fromParams(region, age, employment, smallBusiness);
        model.addAttribute("policies", search.search(condition));
        model.addAttribute("searched", condition.isSearch());
        model.addAttribute("regions", Region.values());
        model.addAttribute("region", region);
        model.addAttribute("age", age);
        model.addAttribute("employment", employment);
        model.addAttribute("smallBusiness", smallBusiness);
        return "index";
    }

    @GetMapping("/policies/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("policy", search.detail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "policy-detail";
    }
}
