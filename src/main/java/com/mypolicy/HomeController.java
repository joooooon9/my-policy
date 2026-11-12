package com.mypolicy;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class HomeController {
    private final List<Policy> policies = List.of(
            new Policy(1, "전국", "청년 주거비 지원", "월세 부담을 덜어주는 청년 주거 지원", 19, 34, false, "소득 조건 확인 필요"),
            new Policy(2, "전북", "재직자 직무교육 지원", "직무 역량을 높이는 교육 과정 지원", 19, 64, true, ""),
            new Policy(3, "익산", "청년 중소기업 근속 지원", "지역 중소기업에 재직 중인 청년 지원", 19, 39, true, "근속기간 확인 필요"));

    @GetMapping("/")
    public String home(@RequestParam(defaultValue = "") String region,
            @RequestParam(required = false) Integer age,
            @RequestParam(defaultValue = "") String employment,
            @RequestParam(defaultValue = "") String smallBusiness, Model model) {
        boolean searched = !region.isBlank() || age != null || !employment.isBlank();
        var results = policies.stream()
                .filter(policy -> region.isBlank() || policy.region().equals("전국")
                        || policy.region().equals(region)
                        || (region.equals("익산") && policy.region().equals("전북")))
                .filter(policy -> age == null || (age >= policy.minAge() && age <= policy.maxAge()))
                .filter(policy -> !policy.employedOnly() || !employment.equals("unemployed"))
                .filter(policy -> policy.id() != 3 || !employment.equals("employed") || !smallBusiness.equals("no"))
                .toList();
        model.addAttribute("policies", results);
        model.addAttribute("searched", searched);
        model.addAttribute("region", region);
        model.addAttribute("age", age);
        model.addAttribute("employment", employment);
        model.addAttribute("smallBusiness", smallBusiness);
        return "index";
    }

    @GetMapping("/policies/{id}")
    public String detail(@PathVariable int id, Model model) {
        var policy = policies.stream().filter(item -> item.id() == id).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("policy", policy);
        return "policy-detail";
    }

    public record Policy(int id, String region, String title, String summary,
            int minAge, int maxAge, boolean employedOnly, String note) {}
}
