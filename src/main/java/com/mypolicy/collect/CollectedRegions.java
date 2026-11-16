package com.mypolicy.collect;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.mypolicy.policy.Region;

/** The regions some source collects now: the only ones the search form offers (CLAUDE.md). */
@Component
public class CollectedRegions {

    private final List<PolicySource> sources;

    public CollectedRegions(List<PolicySource> sources) {
        this.sources = sources;
    }

    /** In Region order (전국 공통, 전북특별자치도, 익산시); empty when no source is configured. */
    public List<Region> list() {
        Set<Region> regions = EnumSet.noneOf(Region.class);
        sources.forEach(s -> regions.addAll(s.regions()));
        return List.copyOf(regions);
    }
}
