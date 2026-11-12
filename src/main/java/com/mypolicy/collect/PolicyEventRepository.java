package com.mypolicy.collect;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyEventRepository extends JpaRepository<PolicyEvent, Long> {

    boolean existsByPolicyIdAndType(Long policyId, PolicyEvent.Type type);

    List<PolicyEvent> findByPolicyIdOrderByIdAsc(Long policyId);
}
