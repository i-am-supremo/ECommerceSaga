package com.saga.orchestrator.dto;

import com.saga.orchestrator.entity.SagaInstance;
import com.saga.orchestrator.entity.SagaStep;
import com.saga.orchestrator.entity.enums.SagaStatus;
import com.saga.orchestrator.entity.enums.StepStatus;
import lombok.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaStatusResponse {

    private UUID sagaId;
    private UUID orderId;
    private SagaStatus status;
    private String currentStep;
    private List<StepInfo> steps;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepInfo {
        private String stepName;
        private StepStatus status;
    }

    public static SagaStatusResponse from(SagaInstance saga, List<SagaStep> steps) {
        return SagaStatusResponse.builder()
                .sagaId(saga.getSagaId())
                .orderId(saga.getOrderId())
                .status(saga.getStatus())
                .currentStep(saga.getCurrentStep())
                .steps(steps.stream()
                        .map(s -> StepInfo.builder().stepName(s.getStepName()).status(s.getStatus()).build())
                        .collect(Collectors.toList()))
                .build();
    }
}