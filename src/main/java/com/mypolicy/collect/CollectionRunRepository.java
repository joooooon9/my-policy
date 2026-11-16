package com.mypolicy.collect;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {

    boolean existsBySourceAndStatus(String source, CollectionRun.Status status);

    List<CollectionRun> findBySourceAndStatus(String source, CollectionRun.Status status);

    Optional<CollectionRun> findFirstBySourceOrderByIdDesc(String source);
}
