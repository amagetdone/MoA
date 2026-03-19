package com.ssafy.amagetdone.domain.plan.application.usecase;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.ssafy.amagetdone.domain.plan.api.dto.PlanListItemResponse;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListPlansUseCase {

	private final PlanRepository planRepository;

	public ListPlansUseCase(PlanRepository planRepository) {
		this.planRepository = planRepository;
	}

	public List<PlanListItemResponse> execute(Integer userId) {
		LocalDate today = LocalDate.now();

		return planRepository.findAllByUser_IdAndStatus(userId, PlanStatus.ACTIVE).stream()
			.sorted(Comparator.comparing(Plan::getPriority)
				.thenComparing(Plan::getTitle, Comparator.nullsLast(String::compareTo)))
			.map(plan -> PlanListItemResponse.from(plan, today))
			.toList();
	}
}
