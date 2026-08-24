package com.saga.orchestrator.repo;

import com.saga.orchestrator.entity.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaStepRepository extends JpaRepository<SagaStep, UUID> {
    List<SagaStep> findBySagaIdOrderByStartedAtAsc(UUID sagaId);
    Optional<SagaStep> findBySagaIdAndStepName(UUID sagaId, String stepName);
}
