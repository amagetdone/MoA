package com.ssafy.amagetdone.domain.plan.application.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

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

	@Transactional
	public void updatePriorities(Integer userId, List<PriorityUpdateCommand> commands) {
		List<Plan> plans = planRepository.findAllByUser_IdAndStatus(userId, PlanStatus.ACTIVE);
		validateCommands(commands, plans);

		Map<Integer, Integer> requestedPrioritiesByPlanId = new HashMap<>();
		for (PriorityUpdateCommand command : commands) {
			requestedPrioritiesByPlanId.put(command.planId(), command.priority());
		}

		plans.forEach(plan -> plan.updatePriority(requestedPrioritiesByPlanId.get(plan.getId())));
	}

	private void validateCommands(List<PriorityUpdateCommand> commands, List<Plan> plans) {
		if (commands == null || commands.isEmpty() || commands.size() != plans.size()) {
			throw new PlanException(PlanErrorCode.INVALID_PRIORITY_UPDATE_REQUEST);
		}

		Set<Integer> ownedPlanIds = new HashSet<>();
		for (Plan plan : plans) {
			ownedPlanIds.add(plan.getId());
		}

		Set<Integer> requestedPlanIds = new HashSet<>();
		Set<Integer> priorities = new HashSet<>();
		for (PriorityUpdateCommand command : commands) {
			if (!ownedPlanIds.contains(command.planId()) || !requestedPlanIds.add(command.planId())) {
				throw new PlanException(PlanErrorCode.INVALID_PRIORITY_UPDATE_REQUEST);
			}
			priorities.add(command.priority());
		}

		for (int expectedPriority = 1; expectedPriority <= commands.size(); expectedPriority++) {
			if (!priorities.contains(expectedPriority)) {
				throw new PlanException(PlanErrorCode.INVALID_PRIORITY_UPDATE_REQUEST);
			}
		}
	}

	public record PriorityUpdateCommand(
		Integer planId,
		Integer priority
	) {
	}
}
