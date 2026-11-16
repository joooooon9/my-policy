package com.mypolicy.policy;

/**
 * What the visitor entered in the custom search. Every field may be empty: an empty field filters nothing.
 * The small-business answer only counts for someone who is employed. keyword is free text: every word must
 * appear in the notice's name, explanation, support text or institution.
 */
public record SearchCondition(Region region, Integer age, Employment employment, SmallBusiness smallBusiness,
        String keyword) {

    public enum Employment {
        ANY, EMPLOYED, UNEMPLOYED;

        static Employment fromParam(String param) {
            return switch (param == null ? "" : param) {
                case "employed" -> EMPLOYED;
                case "unemployed" -> UNEMPLOYED;
                default -> ANY;
            };
        }
    }

    public enum SmallBusiness {
        YES, NO, UNKNOWN;

        static SmallBusiness fromParam(String param) {
            return switch (param == null ? "" : param) {
                case "yes" -> YES;
                case "no" -> NO;
                default -> UNKNOWN;
            };
        }
    }

    public SearchCondition {
        employment = employment == null ? Employment.ANY : employment;
        smallBusiness = employment != Employment.EMPLOYED || smallBusiness == null ? SmallBusiness.UNKNOWN : smallBusiness;
        keyword = keyword == null || keyword.isBlank() ? null : keyword.strip().replaceAll("\\s+", " ");
    }

    public static SearchCondition none() {
        return new SearchCondition(null, null, Employment.ANY, SmallBusiness.UNKNOWN, null);
    }

    /** From the search form's query parameters (region 전국/전북/익산, employment employed/unemployed, smallBusiness yes/no). */
    public static SearchCondition fromParams(String region, Integer age, String employment, String smallBusiness) {
        return fromParams(region, age, employment, smallBusiness, null);
    }

    /** As above, with the keyword (q). */
    public static SearchCondition fromParams(String region, Integer age, String employment, String smallBusiness,
            String keyword) {
        return new SearchCondition(Region.fromParam(region).orElse(null), age,
                Employment.fromParam(employment), SmallBusiness.fromParam(smallBusiness), keyword);
    }

    /** True when the visitor filled in at least one condition (the results are then "맞춤 검색 결과"). */
    public boolean isSearch() {
        return region != null || age != null || employment != Employment.ANY || keyword != null;
    }
}
