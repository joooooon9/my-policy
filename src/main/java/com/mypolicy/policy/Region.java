package com.mypolicy.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Regions the service covers, each with the region above it. Choosing a region shows its own
 * notices and every region above it (Iksan: Iksan, Jeonbuk, nationwide).
 */
public enum Region {
    NATIONWIDE("전국", "전국 공통", null),
    JEONBUK("전북", "전북특별자치도", NATIONWIDE),
    IKSAN("익산", "익산시", JEONBUK);

    private final String param;
    private final String label;
    private final Region parent;

    Region(String param, String label, Region parent) {
        this.param = param;
        this.label = label;
        this.parent = parent;
    }

    /** The value used in the search form and URLs. */
    public String param() {
        return param;
    }

    public String label() {
        return label;
    }

    /** This region and every region above it, nearest first. */
    public List<Region> withParents() {
        List<Region> regions = new ArrayList<>();
        for (Region r = this; r != null; r = r.parent) {
            regions.add(r);
        }
        return regions;
    }

    public static Optional<Region> fromParam(String param) {
        if (param == null || param.isBlank()) {
            return Optional.empty();
        }
        for (Region region : values()) {
            if (region.param.equals(param)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }
}
