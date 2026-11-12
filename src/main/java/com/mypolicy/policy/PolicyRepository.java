package com.mypolicy.policy;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findBySourceAndSourceId(String source, String sourceId);

    List<Policy> findByStatusAndRegionInOrderByApplyEndAscIdAsc(PolicyStatus status, Collection<Region> regions);

    long countBySource(String source);
}
