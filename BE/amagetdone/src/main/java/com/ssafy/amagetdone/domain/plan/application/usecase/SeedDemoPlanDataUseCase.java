package com.ssafy.amagetdone.domain.plan.application.usecase;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedDemoPlanDataUseCase {

	private static final String DEMO_SEED_REASON_PREFIX = "Demo plan seed";
	private static final String DEMO_ALLOCATION_TYPE = "DEMO";

	private final UserRepository userRepository;
	private final PlanRepository planRepository;
	private final BudgetWeekRepository budgetWeekRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final Clock clock;

	public SeedDemoPlanDataUseCase(
		UserRepository userRepository,
		PlanRepository planRepository,
		BudgetWeekRepository budgetWeekRepository,
		PlanWeekAllocationRepository planWeekAllocationRepository,
		BudgetAdjustmentRepository budgetAdjustmentRepository,
		Clock clock
	) {
		this.userRepository = userRepository;
		this.planRepository = planRepository;
		this.budgetWeekRepository = budgetWeekRepository;
		this.planWeekAllocationRepository = planWeekAllocationRepository;
		this.budgetAdjustmentRepository = budgetAdjustmentRepository;
		this.clock = clock;
	}

	@Transactional
	public AccountAdminDto.DemoPlanSeedResponse execute(AccountAdminDto.DemoPlanSeedRequest request) {
		Integer userId = request == null ? null : request.userId();
		if (userId == null || userId <= 0 || request.plans() == null || request.plans().isEmpty()) {
			throw new PlanException(PlanErrorCode.INVALID_PLAN_DEMO_SEED_REQUEST);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new MemberException(MemberErrorCode.USER_NOT_FOUND));
		List<NormalizedPlanSeed> normalizedSeeds = normalize(request.plans());
		List<BudgetWeek> existingWeeks = budgetWeekRepository.findAllByUser_IdOrderByWeekStartDateAsc(userId);
		int replacedPlanCount = replaceExistingPlanData(userId, existingWeeks);

		Map<LocalDate, BudgetWeek> weeksByStartDate = ensureBudgetWeeks(user, normalizedSeeds);
		List<Plan> seededPlans = planRepository.saveAll(
			normalizedSeeds.stream().map(seed -> createPlan(user, seed)).toList()
		);
		Map<String, Plan> planByTitle = seededPlans.stream()
			.collect(LinkedHashMap::new, (map, plan) -> map.put(plan.getTitle(), plan), Map::putAll);

		List<PlanWeekAllocation> allocations = new ArrayList<>();
		List<BudgetAdjustment> adjustments = new ArrayList<>();
		long seededActualAmount = 0L;
		LocalDate today = LocalDate.now(clock);
		for (NormalizedPlanSeed seed : normalizedSeeds) {
			Plan savedPlan = planByTitle.get(seed.title());
			for (NormalizedWeekSeed weekSeed : seed.weeks()) {
				BudgetWeek budgetWeek = weeksByStartDate.get(weekSeed.weekStartDate());
				if (budgetWeek == null) {
					throw new PlanException(PlanErrorCode.BUDGET_WEEK_NOT_FOUND);
				}
				allocations.add(buildAllocation(savedPlan, budgetWeek, weekSeed, today));
				if (weekSeed.actualAmount() > 0L) {
					adjustments.add(buildAdjustment(savedPlan, budgetWeek, weekSeed));
					seededActualAmount += weekSeed.actualAmount();
				}
			}
		}

		planWeekAllocationRepository.saveAll(allocations);
		if (!adjustments.isEmpty()) {
			budgetAdjustmentRepository.saveAll(adjustments);
		}

		refreshBudgetWeekSummaries(weeksByStartDate.values(), allocations, adjustments);

		return new AccountAdminDto.DemoPlanSeedResponse(
			"demo-plan",
			userId,
			replacedPlanCount,
			seededPlans.size(),
			allocations.size(),
			seededActualAmount,
			seededPlans.stream()
				.sorted(Comparator.comparing(Plan::getPriority, Comparator.nullsLast(Integer::compareTo)))
				.map(this::toResult)
				.toList()
		);
	}

	private List<NormalizedPlanSeed> normalize(List<AccountAdminDto.DemoPlanSeedPlanRequest> requests) {
		Set<String> titles = new LinkedHashSet<>();
		List<NormalizedPlanSeed> seeds = new ArrayList<>();
		for (AccountAdminDto.DemoPlanSeedPlanRequest request : requests) {
			if (request == null
				|| request.title() == null
				|| request.title().isBlank()
				|| request.targetAmount() == null
				|| request.targetAmount() <= 0L
				|| request.priority() == null
				|| request.priority() <= 0
				|| request.startDate() == null
				|| request.endDate() == null
				|| request.status() == null
				|| request.weeks() == null
				|| request.weeks().isEmpty()) {
				throw new PlanException(PlanErrorCode.INVALID_PLAN_DEMO_SEED_REQUEST);
			}
			if (!titles.add(request.title().trim())) {
				throw new PlanException(PlanErrorCode.INVALID_PLAN_DEMO_SEED_REQUEST);
			}

			LocalDate originalEndDate = request.originalEndDate() == null ? request.endDate() : request.originalEndDate();
			int expectedWeeks = calculateTargetWeeks(request.startDate(), request.endDate());
			Map<Integer, NormalizedWeekSeed> weeksByNumber = new LinkedHashMap<>();
			long savedAmount = 0L;
			LocalDate alignedWeekStart = request.startDate().with(DayOfWeek.MONDAY);
			for (AccountAdminDto.DemoPlanSeedWeekRequest weekRequest : request.weeks()) {
				if (weekRequest == null
					|| weekRequest.weekNumber() == null
					|| weekRequest.weekNumber() <= 0
					|| weekRequest.plannedAmount() == null
					|| weekRequest.plannedAmount() < 0L
					|| weekRequest.actualAmount() == null
					|| weekRequest.actualAmount() < 0L) {
					throw new PlanException(PlanErrorCode.INVALID_PLAN_DEMO_SEED_REQUEST);
				}
				if (weeksByNumber.containsKey(weekRequest.weekNumber())) {
					throw new PlanException(PlanErrorCode.INVALID_PLAN_DEMO_SEED_REQUEST);
				}
				long finalAmount = weekRequest.finalAmount() == null ? weekRequest.plannedAmount() : weekRequest.finalAmount();
				if (finalAmount < 0L) {
					throw new PlanException(PlanErrorCode.INVALID_PLAN_DEMO_SEED_REQUEST);
				}
				LocalDate weekStartDate = alignedWeekStart.plusWeeks((long)weekRequest.weekNumber() - 1L);
				weeksByNumber.put(
					weekRequest.weekNumber(),
					new NormalizedWeekSeed(
						weekRequest.weekNumber(),
						weekStartDate,
						weekStartDate.plusDays(6L),
						weekRequest.plannedAmount(),
						finalAmount,
						weekRequest.actualAmount()
					)
				);
				savedAmount += weekRequest.actualAmount();
			}

			if (weeksByNumber.size() != expectedWeeks) {
				throw new PlanException(PlanErrorCode.INVALID_PLAN_DEMO_SEED_REQUEST);
			}
			for (int weekNumber = 1; weekNumber <= expectedWeeks; weekNumber++) {
				if (!weeksByNumber.containsKey(weekNumber)) {
					throw new PlanException(PlanErrorCode.INVALID_PLAN_DEMO_SEED_REQUEST);
				}
			}
			if (request.status() == PlanStatus.COMPLETED && savedAmount < request.targetAmount()) {
				throw new PlanException(PlanErrorCode.INVALID_PLAN_DEMO_SEED_REQUEST);
			}

			seeds.add(new NormalizedPlanSeed(
				request.title().trim(),
				request.description(),
				request.targetAmount(),
				request.priority(),
				request.status(),
				request.startDate(),
				request.endDate(),
				originalEndDate,
				savedAmount,
				new ArrayList<>(weeksByNumber.values())
			));
		}
		return seeds.stream()
			.sorted(Comparator.comparing(NormalizedPlanSeed::priority))
			.toList();
	}

	private int replaceExistingPlanData(Integer userId, List<BudgetWeek> existingWeeks) {
		List<Plan> existingPlans = planRepository.findAllByUserIdAndDeletedFalse(userId);
		List<BudgetAdjustment> planAdjustments = existingWeeks.isEmpty()
			? List.of()
			: budgetAdjustmentRepository.findAllByBudgetWeekIn(existingWeeks).stream()
				.filter(adjustment -> adjustment.getPlan() != null)
				.toList();

		if (!planAdjustments.isEmpty()) {
			budgetAdjustmentRepository.deleteAll(planAdjustments);
		}
		planWeekAllocationRepository.deleteByPlanUserId(userId);
		if (!existingPlans.isEmpty()) {
			existingPlans.forEach(Plan::cancelAndDelete);
			planRepository.saveAll(existingPlans);
		}
		resetBudgetWeekSummaries(existingWeeks);
		return existingPlans.size();
	}

	private Map<LocalDate, BudgetWeek> ensureBudgetWeeks(User user, List<NormalizedPlanSeed> seeds) {
		List<BudgetWeek> existingWeeks = budgetWeekRepository.findAllByUser_IdOrderByWeekStartDateAsc(user.getId());
		Map<LocalDate, BudgetWeek> weeksByStartDate = existingWeeks.stream()
			.collect(LinkedHashMap::new, (map, week) -> map.putIfAbsent(week.getWeekStartDate(), week), Map::putAll);

		List<BudgetWeek> missingWeeks = new ArrayList<>();
		for (NormalizedPlanSeed seed : seeds) {
			for (NormalizedWeekSeed weekSeed : seed.weeks()) {
				if (weeksByStartDate.containsKey(weekSeed.weekStartDate())) {
					continue;
				}
				BudgetWeek newWeek = BudgetWeek.builder()
					.user(user)
					.weekSequence(0)
					.weekStartDate(weekSeed.weekStartDate())
					.weekEndDate(weekSeed.weekEndDate())
					.periodDays(7)
					.partial(false)
					.status(BudgetWeekStatus.PLANNED)
					.build();
				missingWeeks.add(newWeek);
				weeksByStartDate.put(weekSeed.weekStartDate(), newWeek);
			}
		}
		if (!missingWeeks.isEmpty()) {
			budgetWeekRepository.saveAll(missingWeeks);
		}

		List<BudgetWeek> orderedWeeks = new ArrayList<>(weeksByStartDate.values());
		orderedWeeks.sort(Comparator.comparing(BudgetWeek::getWeekStartDate));
		for (int index = 0; index < orderedWeeks.size(); index++) {
			orderedWeeks.get(index).setWeekSequence(index + 1);
		}
		budgetWeekRepository.saveAll(orderedWeeks);

		return orderedWeeks.stream()
			.collect(LinkedHashMap::new, (map, week) -> map.put(week.getWeekStartDate(), week), Map::putAll);
	}

	private Plan createPlan(User user, NormalizedPlanSeed seed) {
		Plan plan = Plan.create(
			user,
			seed.title(),
			seed.description(),
			seed.targetAmount(),
			seed.startDate(),
			seed.endDate(),
			seed.priority()
		);
		plan.setOriginalEndDate(seed.originalEndDate());
		plan.setStatus(seed.status());
		plan.setSavedAmount(seed.savedAmount());
		plan.setAutoAllocateEnabled(false);
		return plan;
	}

	private PlanWeekAllocation buildAllocation(
		Plan plan,
		BudgetWeek budgetWeek,
		NormalizedWeekSeed weekSeed,
		LocalDate today
	) {
		Long actualAmount = null;
		if (!budgetWeek.getWeekStartDate().isAfter(today)) {
			actualAmount = weekSeed.actualAmount();
		}
		return PlanWeekAllocation.builder()
			.plan(plan)
			.budgetWeek(budgetWeek)
			.allocationType(DEMO_ALLOCATION_TYPE)
			.plannedAmount(weekSeed.plannedAmount())
			.finalAmount(weekSeed.finalAmount())
			.actualAmount(actualAmount)
			.note("DEMO_SEED_WEEK_%d".formatted(weekSeed.weekNumber()))
			.build();
	}

	private BudgetAdjustment buildAdjustment(Plan plan, BudgetWeek budgetWeek, NormalizedWeekSeed weekSeed) {
		BudgetAdjustment adjustment = BudgetAdjustment.builder()
			.budgetWeek(budgetWeek)
			.plan(plan)
			.adjustmentType(BudgetAdjustmentType.PLAN_SAVED_ADD)
			.adjustmentAmount(weekSeed.actualAmount())
			.reason("%s week-%d".formatted(DEMO_SEED_REASON_PREFIX, weekSeed.weekNumber()))
			.build();
		adjustment.setCreatedAt(LocalDateTime.of(weekSeed.weekEndDate(), LocalDateTime.now(clock).toLocalTime()));
		return adjustment;
	}

	private void resetBudgetWeekSummaries(List<BudgetWeek> budgetWeeks) {
		if (budgetWeeks == null || budgetWeeks.isEmpty()) {
			return;
		}
		budgetWeeks.forEach(week -> {
			week.setPlannedSavingAmount(0L);
			week.setFinalFreeAmount(
				zero(week.getPlannedIncomeAmount())
					- zero(week.getPlannedEssentialAmount())
					- zero(week.getRolloverInAmount())
					+ zero(week.getManualAdjustmentAmount())
			);
		});
		budgetWeekRepository.saveAll(budgetWeeks);
	}

	private void refreshBudgetWeekSummaries(
		Collection<BudgetWeek> affectedWeeks,
		List<PlanWeekAllocation> allocations,
		List<BudgetAdjustment> adjustments
	) {
		if (affectedWeeks == null || affectedWeeks.isEmpty()) {
			return;
		}
		Map<Integer, Long> plannedSavingByWeekId = new LinkedHashMap<>();
		Map<Integer, Long> finalSavingByWeekId = new LinkedHashMap<>();
		Map<Integer, Long> actualSavingByWeekId = new LinkedHashMap<>();

		for (PlanWeekAllocation allocation : allocations) {
			Integer weekId = allocation.getBudgetWeek() == null ? null : allocation.getBudgetWeek().getId();
			if (weekId == null) {
				continue;
			}
			plannedSavingByWeekId.merge(weekId, zero(allocation.getPlannedAmount()), Long::sum);
			finalSavingByWeekId.merge(weekId, zero(allocation.getFinalAmount()), Long::sum);
		}
		for (BudgetAdjustment adjustment : adjustments) {
			Integer weekId = adjustment.getBudgetWeek() == null ? null : adjustment.getBudgetWeek().getId();
			if (weekId == null) {
				continue;
			}
			actualSavingByWeekId.merge(weekId, zero(adjustment.getAdjustmentAmount()), Long::sum);
		}

		List<BudgetWeek> weeksToSave = new ArrayList<>();
		for (BudgetWeek budgetWeek : affectedWeeks) {
			Integer weekId = budgetWeek.getId();
			long plannedSaving = plannedSavingByWeekId.getOrDefault(weekId, 0L);
			long finalSaving = finalSavingByWeekId.getOrDefault(weekId, 0L);
			long actualSaving = actualSavingByWeekId.getOrDefault(weekId, 0L);

			budgetWeek.setPlannedSavingAmount(plannedSaving);
			budgetWeek.setFinalFreeAmount(
				zero(budgetWeek.getPlannedIncomeAmount())
					- zero(budgetWeek.getPlannedEssentialAmount())
					- finalSaving
					- zero(budgetWeek.getRolloverInAmount())
					+ zero(budgetWeek.getManualAdjustmentAmount())
					- actualSaving
			);
			weeksToSave.add(budgetWeek);
		}
		budgetWeekRepository.saveAll(weeksToSave);
	}

	private AccountAdminDto.DemoPlanSeedPlanResult toResult(Plan plan) {
		return new AccountAdminDto.DemoPlanSeedPlanResult(
			plan.getId(),
			plan.getTitle(),
			plan.getStatus(),
			plan.getSavedAmount(),
			plan.getTargetAmount(),
			plan.getStartDate(),
			plan.getEndDate(),
			plan.getOriginalEndDate() == null ? plan.getEndDate() : plan.getOriginalEndDate(),
			plan.getTargetWeeks()
		);
	}

	private int calculateTargetWeeks(LocalDate startDate, LocalDate endDate) {
		if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
			throw new PlanException(PlanErrorCode.INVALID_PLAN_DEMO_SEED_REQUEST);
		}
		long durationDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1L;
		return (int)((durationDays + 6L) / 7L);
	}

	private long zero(Long amount) {
		return amount == null ? 0L : amount;
	}

	private record NormalizedPlanSeed(
		String title,
		String description,
		Long targetAmount,
		Integer priority,
		PlanStatus status,
		LocalDate startDate,
		LocalDate endDate,
		LocalDate originalEndDate,
		Long savedAmount,
		List<NormalizedWeekSeed> weeks
	) {
	}

	private record NormalizedWeekSeed(
		Integer weekNumber,
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		Long plannedAmount,
		Long finalAmount,
		Long actualAmount
	) {
	}
}
