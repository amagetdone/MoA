package com.ssafy.amagetdone.domain.plan.application.service;

import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanPriorityService {

	private final PlanRepository planRepository;

	public PlanPriorityService(PlanRepository planRepository) {
		this.planRepository = planRepository;
	}

	@Transactional
	public void shiftPriorities(Integer userId, Integer priority) {
		planRepository.findAllByUserIdAndPriorityGreaterThanEqualOrderByPriorityAsc(userId, priority)
			.forEach(plan -> plan.shiftPriority(1));
	}
}
