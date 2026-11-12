package com.mypolicy.collect;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRunRepository extends JpaRepository<CollectionRun, Long> {

    boolean existsBySourceAndStatus(String source, CollectionRun.Status status);
}
