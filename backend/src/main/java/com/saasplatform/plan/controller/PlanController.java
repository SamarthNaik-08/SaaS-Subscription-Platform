package com.saasplatform.plan.controller;

import com.saasplatform.common.dto.ApiResponse;
import com.saasplatform.plan.dto.PlanDto;
import com.saasplatform.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanDto>>> getAllActivePlans() {
        log.info("Fetching all active plans");
        List<PlanDto> plans = planRepository.findAll().stream()
                .filter(p -> p.isActive())
                .map(PlanDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(plans, "Active plans retrieved successfully"));
    }
}
