package com.ssafy.amagetdone.domain.budget.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ssafy.amagetdone.domain.account.model.repository.PocketRepository;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetAdjustment;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.BudgetWeek;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialExpenseMonth;
import com.ssafy.amagetdone.domain.budget.model.entity.EssentialWeekAllocation;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetAdjustmentRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetRolloverRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.BudgetWeekRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialExpenseMonthRepository;
import com.ssafy.amagetdone.domain.budget.model.repository.EssentialWeekAllocationRepository;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetAdjustmentType;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetMonthStatus;
import com.ssafy.amagetdone.domain.budget.model.vo.BudgetWeekStatus;
import com.ssafy.amagetdone.domain.budget.model.vo.ExpenseScope;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.entity.PlanWeekAllocation;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanWeekAllocationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BudgetAllocationService {

	private static final String AUTO_ALLOCATION_TYPE = "AUTO";

	private final BudgetAdjustmentRepository budgetAdjustmentRepository;
	private final BudgetMonthRepository budgetMonthRepository;
	private final BudgetWeekRepository budgetWeekRepository;
	private final EssentialExpenseMonthRepository essentialExpenseMonthRepository;
	private final EssentialWeekAllocationRepository essentialWeekAllocationRepository;
	private final PlanWeekAllocationRepository planWeekAllocationRepository;
	private final BudgetRolloverRepository budgetRolloverRepository;
	private final PocketRepository pocketRepository;
	private final PlanRepository planRepository;

	public AllocationResult allocateBudget(
		User user,
		LocalDate serviceStartDate,
		Long monthlyIncome,
		List<EssentialExpenseMonth> essentials,
		List<Plan> activePlans
	) {
		return allocateBudget(user, serviceStartDate, monthlyIncome, essentials, activePlans, null);
	}

	public AllocationResult allocateBudget(
		User user,
		LocalDate serviceStartDate,
		Long monthlyIncome,
		List<EssentialExpenseMonth> essentials,
		List<Plan> activePlans,
		LocalDate allocationEndDateOverride
	) {
		Objects.requireNonNull(user, "user must not be null");
		Objects.requireNonNull(serviceStartDate, "serviceStartDate must not be null");

		long normalizedMonthlyIncome = normalizeAmount(monthlyIncome);
		List<EssentialExpenseMonth> normalizedEssentials = normalizeEssentials(user, essentials);
		List<Plan> normalizedPlans = normalizePlans(activePlans);

		LocalDate allocationEndDate = allocationEndDateOverride != null
			? allocationEndDateOverride
			: resolveAllocationEnd(serviceStartDate, normalizedEssentials, normalizedPlans);
		if (allocationEndDate.isBefore(serviceStartDate)) {
			return new AllocationResult(List.of(), List.of(), List.of(), List.of(), List.of());
		}
		List<WeekFrame> weekFrames = buildWeekFrames(serviceStartDate, allocationEndDate);
		Map<LocalDate, BudgetMonth> budgetMonthsByBaseDate = upsertBudgetMonths(
			user,
			normalizedMonthlyIncome,
			normalizedEssentials,
			serviceStartDate,
			allocationEndDate
		);

		List<BudgetWeek> overlappingBudgetWeeks = findOverlappingBudgetWeeks(user, weekFrames);
		Map<EssentialAllocationKey, ExistingEssentialAllocationState> existingEssentialStates = loadExistingEssentialStates(
			overlappingBudgetWeeks
		);
		Map<PlanAllocationKey, ExistingPlanAllocationState> existingPlanStates = loadExistingPlanStates(
			overlappingBudgetWeeks
		);
		deleteExistingAllocations(overlappingBudgetWeeks);
		Map<LocalDate, BudgetWeek> budgetWeeksByStartDate = upsertBudgetWeeks(user, weekFrames, overlappingBudgetWeeks);

		Map<LocalDate, Long> incomeByWeekStartDate = allocateMonthlyIncome(
			serviceStartDate,
			allocationEndDate,
			normalizedMonthlyIncome,
			weekFrames
		);
		List<EssentialWeekAllocation> essentialAllocations = buildEssentialAllocations(
			normalizedEssentials,
			weekFrames,
			budgetWeeksByStartDate,
			existingEssentialStates
		);
		Map<LocalDate, Long> essentialByWeekStartDate = sumEssentialsByWeek(essentialAllocations);
		PlanAllocationBundle planAllocationBundle = buildPlanAllocations(
			normalizedPlans,
			weekFrames,
			budgetWeeksByStartDate,
			serviceStartDate,
			allocationEndDate,
			existingPlanStates
		);

		applyShortageAdjustments(
			weekFrames,
			budgetWeeksByStartDate,
			incomeByWeekStartDate,
			essentialByWeekStartDate,
			planAllocationBundle.allocationsByWeekStartDate(),
			planAllocationBundle.shortageSummaries()
		);

		List<BudgetWeek> budgetWeeks = updateBudgetWeeks(
			weekFrames,
			budgetWeeksByStartDate,
			incomeByWeekStartDate,
			essentialByWeekStartDate,
			planAllocationBundle.allocationsByWeekStartDate()
		);
		List<PlanWeekAllocation> planAllocations = flattenPlanAllocations(weekFrames, planAllocationBundle.allocationsByWeekStartDate());

		essentialWeekAllocationRepository.saveAll(essentialAllocations);
		planWeekAllocationRepository.saveAll(planAllocations);

		return new AllocationResult(
			new ArrayList<>(budgetMonthsByBaseDate.values()),
			budgetWeeks,
			essentialAllocations,
			planAllocations,
			new ArrayList<>(planAllocationBundle.shortageSummaries().values())
		);
	}

	private List<EssentialExpenseMonth> normalizeEssentials(User user, List<EssentialExpenseMonth> essentials) {
		List<EssentialExpenseMonth> source = essentials;
		if (source == null || source.isEmpty()) {
			source = essentialExpenseMonthRepository.findAllByBudgetMonth_User_IdOrderByBudgetMonth_BaseDateAscIdAsc(user.getId());
		}

		return source.stream()
			.filter(Objects::nonNull)
			.filter(essential -> essential.getBudgetMonth() != null)
			.filter(essential -> essential.getBudgetMonth().getBaseDate() != null)
			.filter(essential -> essential.getCategory() != null)
			.toList();
	}

	private List<Plan> normalizePlans(List<Plan> activePlans) {
		if (activePlans == null || activePlans.isEmpty()) {
			return List.of();
		}

		return activePlans.stream()
			.filter(Objects::nonNull)
			.filter(Plan::isAutoAllocateEnabled)
			.filter(plan -> plan.getEndDate() != null)
			.filter(plan -> !plan.getEndDate().isBefore(plan.getStartDate() != null ? plan.getStartDate() : plan.getEndDate()))
			.toList();
	}

	private LocalDate resolveAllocationEnd(
		LocalDate serviceStartDate,
		List<EssentialExpenseMonth> essentials,
		List<Plan> activePlans
	) {
		LocalDate allocationEndDate = serviceStartDate.withDayOfMonth(serviceStartDate.lengthOfMonth());

		for (EssentialExpenseMonth essential : essentials) {
			LocalDate monthBaseDate = monthStart(essential.getBudgetMonth().getBaseDate());
			LocalDate monthEndDate = monthBaseDate.withDayOfMonth(monthBaseDate.lengthOfMonth());
			if (monthEndDate.isAfter(allocationEndDate)) {
				allocationEndDate = monthEndDate;
			}
		}

		for (Plan plan : activePlans) {
			if (plan.getEndDate().isAfter(allocationEndDate)) {
				allocationEndDate = plan.getEndDate();
			}
		}

		return allocationEndDate;
	}

	private Map<LocalDate, BudgetMonth> upsertBudgetMonths(
		User user,
		long monthlyIncome,
		List<EssentialExpenseMonth> essentials,
		LocalDate serviceStartDate,
		LocalDate allocationEndDate
	) {
		Map<LocalDate, MonthlyEssentialTotal> totalsByMonth = aggregateEssentialTotals(essentials);
		Map<LocalDate, BudgetMonth> referencedBudgetMonths = new HashMap<>();

		for (EssentialExpenseMonth essential : essentials) {
			BudgetMonth referencedBudgetMonth = essential.getBudgetMonth();
			LocalDate monthBaseDate = monthStart(referencedBudgetMonth.getBaseDate());
			referencedBudgetMonths.putIfAbsent(monthBaseDate, referencedBudgetMonth);
		}

		List<BudgetMonth> budgetMonths = new ArrayList<>();
		LocalDate currentMonthStart = monthStart(serviceStartDate);

		while (!currentMonthStart.isAfter(monthStart(allocationEndDate))) {
			LocalDate monthBaseDate = currentMonthStart;
			BudgetMonth budgetMonth = referencedBudgetMonths.get(monthBaseDate);
			if (budgetMonth == null) {
				budgetMonth = budgetMonthRepository.findFirstByUserAndBaseDateOrderByIdDesc(user, monthBaseDate)
					.orElseGet(() -> BudgetMonth.builder()
						.user(user)
						.baseDate(monthBaseDate)
						.status(BudgetMonthStatus.OPEN)
						.build());
			}

			MonthlyEssentialTotal monthlyEssentialTotal = totalsByMonth.getOrDefault(monthBaseDate, MonthlyEssentialTotal.ZERO);
			budgetMonth.setUser(user);
			budgetMonth.setBaseDate(monthBaseDate);
			budgetMonth.setTotalIncomeAmount(monthlyIncome);
			budgetMonth.setTotalFixedEssentialAmount(monthlyEssentialTotal.fixedAmount());
			budgetMonth.setTotalVariableEssentialAmount(monthlyEssentialTotal.variableAmount());
			if (budgetMonth.getStatus() == null) {
				budgetMonth.setStatus(BudgetMonthStatus.OPEN);
			}
			budgetMonths.add(budgetMonth);
			currentMonthStart = currentMonthStart.plusMonths(1);
		}

		List<BudgetMonth> savedBudgetMonths = budgetMonthRepository.saveAll(budgetMonths);
		Map<LocalDate, BudgetMonth> budgetMonthsByBaseDate = new LinkedHashMap<>();
		for (BudgetMonth savedBudgetMonth : savedBudgetMonths) {
			budgetMonthsByBaseDate.put(savedBudgetMonth.getBaseDate(), savedBudgetMonth);
		}
		return budgetMonthsByBaseDate;
	}

	private List<BudgetWeek> findOverlappingBudgetWeeks(User user, List<WeekFrame> weekFrames) {
		LocalDate firstWeekStartDate = weekFrames.get(0).startDate();
		LocalDate lastWeekEndDate = weekFrames.get(weekFrames.size() - 1).endDate();
		return budgetWeekRepository
			.findAllByUserIdAndWeekEndDateGreaterThanEqualAndWeekStartDateLessThanEqualOrderByWeekStartDateAscIdDesc(
				user.getId(),
				firstWeekStartDate,
				lastWeekEndDate
			);
	}

	private Map<LocalDate, BudgetWeek> upsertBudgetWeeks(User user, List<WeekFrame> weekFrames, List<BudgetWeek> overlappingBudgetWeeks) {
		LocalDate firstWeekStartDate = weekFrames.get(0).startDate();
		Map<LocalDate, BudgetWeek> existingWeeksByStartDate = selectReusableWeeksByStartDate(overlappingBudgetWeeks);
		int nextSequence = determineStartingSequence(user, firstWeekStartDate, overlappingBudgetWeeks);
		List<BudgetWeek> budgetWeeksToSave = new ArrayList<>();

		for (WeekFrame weekFrame : weekFrames) {
			BudgetWeek budgetWeek = existingWeeksByStartDate.get(weekFrame.startDate());
			if (budgetWeek == null) {
				budgetWeek = BudgetWeek.builder()
					.user(user)
					.weekStartDate(weekFrame.startDate())
					.weekEndDate(weekFrame.endDate())
					.partial(weekFrame.partial())
					.periodDays(weekFrame.periodDays())
					.status(BudgetWeekStatus.PLANNED)
					.build();
			}

			budgetWeek.setUser(user);
			budgetWeek.setWeekSequence(nextSequence++);
			budgetWeek.setWeekStartDate(weekFrame.startDate());
			budgetWeek.setWeekEndDate(weekFrame.endDate());
			budgetWeek.setPeriodDays(weekFrame.periodDays());
			budgetWeek.setPartial(weekFrame.partial());
			if (budgetWeek.getStatus() == null) {
				budgetWeek.setStatus(BudgetWeekStatus.PLANNED);
			}
			budgetWeeksToSave.add(budgetWeek);
		}

		List<BudgetWeek> savedBudgetWeeks = budgetWeekRepository.saveAll(budgetWeeksToSave);
		deleteObsoleteBudgetWeeks(overlappingBudgetWeeks, savedBudgetWeeks);
		Map<LocalDate, BudgetWeek> budgetWeeksByStartDate = new LinkedHashMap<>();
		for (BudgetWeek savedBudgetWeek : savedBudgetWeeks) {
			budgetWeeksByStartDate.put(savedBudgetWeek.getWeekStartDate(), savedBudgetWeek);
		}
		return budgetWeeksByStartDate;
	}

	private int determineStartingSequence(
		User user,
		LocalDate firstWeekStartDate,
		List<BudgetWeek> overlappingBudgetWeeks
	) {
		if (!overlappingBudgetWeeks.isEmpty()) {
			return overlappingBudgetWeeks.stream()
				.min(Comparator.comparing(BudgetWeek::getWeekStartDate)
					.thenComparing(BudgetWeek::getId, Comparator.nullsLast(Integer::compareTo)))
				.map(BudgetWeek::getWeekSequence)
				.filter(Objects::nonNull)
				.orElse(1);
		}

		return budgetWeekRepository.findFirstByUserOrderByWeekEndDateDesc(user)
			.filter(budgetWeek -> budgetWeek.getWeekEndDate().isBefore(firstWeekStartDate))
			.map(BudgetWeek::getWeekSequence)
			.map(sequence -> sequence + 1)
			.orElse(1);
	}

	private Map<LocalDate, BudgetWeek> selectReusableWeeksByStartDate(List<BudgetWeek> overlappingBudgetWeeks) {
		Map<LocalDate, BudgetWeek> reusableWeeksByStartDate = new LinkedHashMap<>();
		for (BudgetWeek budgetWeek : overlappingBudgetWeeks) {
			reusableWeeksByStartDate.putIfAbsent(budgetWeek.getWeekStartDate(), budgetWeek);
		}
		return reusableWeeksByStartDate;
	}

	private void deleteObsoleteBudgetWeeks(List<BudgetWeek> overlappingBudgetWeeks, List<BudgetWeek> savedBudgetWeeks) {
		Set<Integer> retainedIds = savedBudgetWeeks.stream()
			.map(BudgetWeek::getId)
			.filter(Objects::nonNull)
			.collect(HashSet::new, Set::add, Set::addAll);
		List<BudgetWeek> obsoleteBudgetWeeks = overlappingBudgetWeeks.stream()
			.filter(budgetWeek -> budgetWeek.getId() != null)
			.filter(budgetWeek -> !retainedIds.contains(budgetWeek.getId()))
			.toList();
		if (!obsoleteBudgetWeeks.isEmpty()) {
			List<BudgetAdjustment> obsoleteAdjustments = budgetAdjustmentRepository.findAllByBudgetWeekIn(obsoleteBudgetWeeks);
			restoreSavedAmounts(obsoleteAdjustments);
			if (!obsoleteAdjustments.isEmpty()) {
				budgetAdjustmentRepository.deleteAll(obsoleteAdjustments);
			}
			budgetRolloverRepository.deleteAllBySourceBudgetWeekIn(obsoleteBudgetWeeks);
			budgetRolloverRepository.deleteAllByTargetBudgetWeekIn(obsoleteBudgetWeeks);
			pocketRepository.deleteAllByBudgetWeekIn(obsoleteBudgetWeeks);
			budgetWeekRepository.deleteAll(obsoleteBudgetWeeks);
		}
	}

	private void restoreSavedAmounts(List<BudgetAdjustment> adjustments) {
		Map<Integer, Long> savedAmountDeltasByPlanId = new LinkedHashMap<>();
		for (BudgetAdjustment adjustment : adjustments) {
			if (adjustment.getPlan() == null || adjustment.getPlan().getId() == null) {
				continue;
			}
			long signedAmount = signedSavedAdjustmentAmount(adjustment);
			if (signedAmount == 0L) {
				continue;
			}
			savedAmountDeltasByPlanId.merge(adjustment.getPlan().getId(), -signedAmount, Long::sum);
		}
		savedAmountDeltasByPlanId.forEach(planRepository::adjustSavedAmount);
	}

	private long signedSavedAdjustmentAmount(BudgetAdjustment adjustment) {
		return switch (adjustment.getAdjustmentType()) {
			case PLAN_SAVED_ADD, PLAN_SAVED_AUTO_ADD -> normalizeAmount(adjustment.getAdjustmentAmount());
			case PLAN_SAVED_AUTO_SUBTRACT -> -normalizeAmount(adjustment.getAdjustmentAmount());
			default -> 0L;
		};
	}

	private Map<EssentialAllocationKey, ExistingEssentialAllocationState> loadExistingEssentialStates(
		Collection<BudgetWeek> budgetWeeks
	) {
		Map<EssentialAllocationKey, ExistingEssentialAllocationState> existingStates = new HashMap<>();
		for (BudgetWeek budgetWeek : budgetWeeks) {
			for (EssentialWeekAllocation allocation : essentialWeekAllocationRepository.findByBudgetWeek(budgetWeek)) {
				ExistingEssentialAllocationState existingState = new ExistingEssentialAllocationState(
					normalizeAmount(allocation.getActualSpentAmount()),
					allocation.getNote()
				);
				existingStates.put(EssentialAllocationKey.from(budgetWeek.getWeekStartDate(), allocation), existingState);
			}
		}
		return existingStates;
	}

	private Map<PlanAllocationKey, ExistingPlanAllocationState> loadExistingPlanStates(
		Collection<BudgetWeek> budgetWeeks
	) {
		Map<PlanAllocationKey, ExistingPlanAllocationState> existingStates = new HashMap<>();
		for (BudgetWeek budgetWeek : budgetWeeks) {
			for (PlanWeekAllocation allocation : planWeekAllocationRepository.findByBudgetWeek(budgetWeek)) {
				if (allocation.getPlan() == null || allocation.getPlan().getId() == null) {
					continue;
				}
				existingStates.put(
					new PlanAllocationKey(allocation.getPlan().getId(), budgetWeek.getWeekStartDate()),
					new ExistingPlanAllocationState(allocation.getActualAmount(), allocation.getNote())
				);
			}
		}
		return existingStates;
	}

	private void deleteExistingAllocations(Collection<BudgetWeek> budgetWeeks) {
		List<EssentialWeekAllocation> essentialAllocations = new ArrayList<>();
		List<PlanWeekAllocation> planAllocations = new ArrayList<>();

		for (BudgetWeek budgetWeek : budgetWeeks) {
			essentialAllocations.addAll(essentialWeekAllocationRepository.findByBudgetWeek(budgetWeek));
			planAllocations.addAll(planWeekAllocationRepository.findByBudgetWeek(budgetWeek));
		}

		if (!essentialAllocations.isEmpty()) {
			essentialWeekAllocationRepository.deleteAll(essentialAllocations);
		}
		if (!planAllocations.isEmpty()) {
			planWeekAllocationRepository.deleteAll(planAllocations);
		}
	}

	private Map<LocalDate, Long> allocateMonthlyIncome(
		LocalDate serviceStartDate,
		LocalDate allocationEndDate,
		long monthlyIncome,
		List<WeekFrame> weekFrames
	) {
		Map<LocalDate, Long> incomeByWeekStartDate = initializeWeeklyTotals(weekFrames);
		LocalDate currentMonthStart = monthStart(serviceStartDate);

		while (!currentMonthStart.isAfter(monthStart(allocationEndDate))) {
			LocalDate monthEndDate = currentMonthStart.withDayOfMonth(currentMonthStart.lengthOfMonth());
			List<WeightedWeekFrame> slices = slicesForPeriod(currentMonthStart, monthEndDate, weekFrames);
			Map<LocalDate, Long> distributedAmount = distributeByDenominator(monthlyIncome, currentMonthStart.lengthOfMonth(), slices);
			mergeWeeklyTotals(incomeByWeekStartDate, distributedAmount);
			currentMonthStart = currentMonthStart.plusMonths(1);
		}

		return incomeByWeekStartDate;
	}

	private List<EssentialWeekAllocation> buildEssentialAllocations(
		List<EssentialExpenseMonth> essentials,
		List<WeekFrame> weekFrames,
		Map<LocalDate, BudgetWeek> budgetWeeksByStartDate,
		Map<EssentialAllocationKey, ExistingEssentialAllocationState> existingEssentialStates
	) {
		List<EssentialWeekAllocation> essentialAllocations = new ArrayList<>();

		for (EssentialExpenseMonth essential : essentials) {
			LocalDate monthStartDate = monthStart(essential.getBudgetMonth().getBaseDate());
			LocalDate monthEndDate = monthStartDate.withDayOfMonth(monthStartDate.lengthOfMonth());
			List<WeightedWeekFrame> slices = slicesForPeriod(monthStartDate, monthEndDate, weekFrames);
			Map<LocalDate, Long> distributedAmounts = distributeByDenominator(
				normalizeAmount(essential.getAmount()),
				monthStartDate.lengthOfMonth(),
				slices
			);

			for (Map.Entry<LocalDate, Long> entry : distributedAmounts.entrySet()) {
				long plannedAmount = entry.getValue();
				if (plannedAmount <= 0L) {
					continue;
				}

				BudgetWeek budgetWeek = budgetWeeksByStartDate.get(entry.getKey());
				EssentialAllocationKey key = EssentialAllocationKey.from(entry.getKey(), essential);
				ExistingEssentialAllocationState existingState = existingEssentialStates.get(key);
				essentialAllocations.add(EssentialWeekAllocation.builder()
					.essentialExpenseMonth(essential)
					.category(essential.getCategory())
					.budgetWeek(budgetWeek)
					.expenseScope(parseExpenseScope(essential.getExpenseScope()))
					.plannedAmount(plannedAmount)
					.actualSpentAmount(existingState != null ? existingState.actualSpentAmount() : 0L)
					.note(existingState != null ? existingState.note() : null)
					.build());
			}
		}

		return essentialAllocations;
	}

	private Map<LocalDate, Long> sumEssentialsByWeek(List<EssentialWeekAllocation> essentialAllocations) {
		Map<LocalDate, Long> totalsByWeekStartDate = new HashMap<>();
		for (EssentialWeekAllocation allocation : essentialAllocations) {
			LocalDate weekStartDate = allocation.getBudgetWeek().getWeekStartDate();
			totalsByWeekStartDate.merge(weekStartDate, normalizeAmount(allocation.getPlannedAmount()), Long::sum);
		}
		return totalsByWeekStartDate;
	}

	private PlanAllocationBundle buildPlanAllocations(
		List<Plan> activePlans,
		List<WeekFrame> weekFrames,
		Map<LocalDate, BudgetWeek> budgetWeeksByStartDate,
		LocalDate serviceStartDate,
		LocalDate allocationEndDate,
		Map<PlanAllocationKey, ExistingPlanAllocationState> existingPlanStates
	) {
		Map<LocalDate, List<PlanWeekAllocation>> allocationsByWeekStartDate = new LinkedHashMap<>();
		for (WeekFrame weekFrame : weekFrames) {
			allocationsByWeekStartDate.put(weekFrame.startDate(), new ArrayList<>());
		}

		Map<Plan, PlanShortageSummary> shortageSummaries = new LinkedHashMap<>();
		Map<Integer, PriorPlanAllocationState> priorAllocationStates = loadPriorPlanAllocationStates(activePlans, serviceStartDate);

		for (Plan plan : activePlans) {
			long targetAmount = normalizeAmount(plan.getTargetAmount());
			long totalPlanDays = (long)plan.getTargetWeeks() * 7L;
			long baseWeeklyAmount = calculateBaseWeeklyAmount(targetAmount, plan.getTargetWeeks());
			if (baseWeeklyAmount <= 0L) {
				continue;
			}

			LocalDate originalPlanStartDate = defaultDate(plan.getStartDate(), serviceStartDate);
			LocalDate planStartDate = maxDate(serviceStartDate, originalPlanStartDate);
			LocalDate planEndDate = minDate(allocationEndDate, plan.getEndDate());
			if (planEndDate.isBefore(planStartDate)) {
				continue;
			}

			List<WeightedWeekFrame> slices = slicesForPeriod(planStartDate, planEndDate, weekFrames);
			if (slices.isEmpty()) {
				continue;
			}

			long elapsedDaysBeforeSegment = elapsedPlanDays(originalPlanStartDate, plan.getEndDate(), planStartDate.minusDays(1L));
			long remainingPlanDays = Math.max(totalPlanDays - elapsedDaysBeforeSegment, 0L);
			if (remainingPlanDays <= 0L) {
				continue;
			}

			PriorPlanAllocationState priorAllocationState = priorAllocationStates.getOrDefault(
				plan.getId(),
				PriorPlanAllocationState.EMPTY
			);
			long allocatedBeforeSegment = priorAllocationState.exists()
				? Math.min(priorAllocationState.finalAmount(), targetAmount)
				: proratedAmount(targetAmount, totalPlanDays, elapsedDaysBeforeSegment);
			long remainingAmount = Math.max(targetAmount - allocatedBeforeSegment, 0L);
			Map<LocalDate, Long> distributedAmounts = distributeByDenominator(remainingAmount, remainingPlanDays, slices);
			shortageSummaries.put(plan, new PlanShortageSummary(plan, baseWeeklyAmount, 0L, 0));

			for (Map.Entry<LocalDate, Long> entry : distributedAmounts.entrySet()) {
				long plannedAmount = entry.getValue();
				if (plannedAmount <= 0L) {
					continue;
				}

				BudgetWeek budgetWeek = budgetWeeksByStartDate.get(entry.getKey());
				PlanAllocationKey planKey = new PlanAllocationKey(plan.getId(), entry.getKey());
				ExistingPlanAllocationState existingState = existingPlanStates.get(planKey);
				allocationsByWeekStartDate.get(entry.getKey()).add(PlanWeekAllocation.builder()
					.budgetWeek(budgetWeek)
					.plan(plan)
					.allocationType(AUTO_ALLOCATION_TYPE)
					.plannedAmount(plannedAmount)
					.finalAmount(plannedAmount)
					.actualAmount(existingState != null ? existingState.actualAmount() : null)
					.note(existingState != null ? existingState.note() : null)
					.build());
			}
		}

		return new PlanAllocationBundle(allocationsByWeekStartDate, shortageSummaries);
	}

	private Map<Integer, PriorPlanAllocationState> loadPriorPlanAllocationStates(List<Plan> activePlans, LocalDate serviceStartDate) {
		List<Integer> planIds = activePlans.stream()
			.map(Plan::getId)
			.filter(Objects::nonNull)
			.toList();
		if (planIds.isEmpty()) {
			return Map.of();
		}

		Map<Integer, PriorPlanAllocationState> priorStatesByPlanId = new HashMap<>();
		for (PlanWeekAllocation allocation : planWeekAllocationRepository
			.findAllByPlanIdInAndBudgetWeekWeekStartDateBefore(planIds, serviceStartDate)) {
			if (allocation.getPlan() == null || allocation.getPlan().getId() == null) {
				continue;
			}
			priorStatesByPlanId.merge(
				allocation.getPlan().getId(),
				new PriorPlanAllocationState(normalizeAmount(allocation.getFinalAmount()), true),
				(left, right) -> new PriorPlanAllocationState(left.finalAmount() + right.finalAmount(), true)
			);
		}
		return priorStatesByPlanId;
	}

	private void applyShortageAdjustments(
		List<WeekFrame> weekFrames,
		Map<LocalDate, BudgetWeek> budgetWeeksByStartDate,
		Map<LocalDate, Long> incomeByWeekStartDate,
		Map<LocalDate, Long> essentialByWeekStartDate,
		Map<LocalDate, List<PlanWeekAllocation>> planAllocationsByWeekStartDate,
		Map<Plan, PlanShortageSummary> shortageSummaries
	) {
		for (WeekFrame weekFrame : weekFrames) {
			LocalDate weekStartDate = weekFrame.startDate();
			BudgetWeek budgetWeek = budgetWeeksByStartDate.get(weekStartDate);
			long plannedIncomeAmount = incomeByWeekStartDate.getOrDefault(weekStartDate, 0L);
			long plannedEssentialAmount = essentialByWeekStartDate.getOrDefault(weekStartDate, 0L);
			List<PlanWeekAllocation> planAllocations = planAllocationsByWeekStartDate.getOrDefault(weekStartDate, List.of());
			long plannedSavingAmount = planAllocations.stream().mapToLong(allocation -> normalizeAmount(allocation.getPlannedAmount())).sum();
			long plannedFreeAmount = plannedIncomeAmount - plannedEssentialAmount - plannedSavingAmount;
			long planSavedAmount = sumPlanSavedAmount(budgetWeek);
			long currentFreeAmount = plannedFreeAmount - normalizeAmount(budgetWeek.getRolloverInAmount())
				+ normalizeAmount(budgetWeek.getManualAdjustmentAmount())
				- planSavedAmount;

			if (currentFreeAmount < 0L && !planAllocations.isEmpty()) {
				long shortage = -currentFreeAmount;
				for (PlanWeekAllocation planAllocation : planAllocations.stream().sorted(planShortageOrder()).toList()) {
					long deduction = Math.min(shortage, normalizeAmount(planAllocation.getFinalAmount()));
					if (deduction <= 0L) {
						continue;
					}

					planAllocation.setFinalAmount(planAllocation.getFinalAmount() - deduction);
					shortage -= deduction;
					updateShortageSummary(shortageSummaries, planAllocation.getPlan(), deduction);
					if (shortage == 0L) {
						break;
					}
				}
			}
		}
	}

	private List<BudgetWeek> updateBudgetWeeks(
		List<WeekFrame> weekFrames,
		Map<LocalDate, BudgetWeek> budgetWeeksByStartDate,
		Map<LocalDate, Long> incomeByWeekStartDate,
		Map<LocalDate, Long> essentialByWeekStartDate,
		Map<LocalDate, List<PlanWeekAllocation>> planAllocationsByWeekStartDate
	) {
		List<BudgetWeek> budgetWeeks = new ArrayList<>();

		for (WeekFrame weekFrame : weekFrames) {
			LocalDate weekStartDate = weekFrame.startDate();
			BudgetWeek budgetWeek = budgetWeeksByStartDate.get(weekStartDate);
			long plannedIncomeAmount = incomeByWeekStartDate.getOrDefault(weekStartDate, 0L);
			long plannedEssentialAmount = essentialByWeekStartDate.getOrDefault(weekStartDate, 0L);
			long plannedSavingAmount = planAllocationsByWeekStartDate.getOrDefault(weekStartDate, List.of()).stream()
				.mapToLong(allocation -> normalizeAmount(allocation.getPlannedAmount()))
				.sum();
			long finalSavingAmount = planAllocationsByWeekStartDate.getOrDefault(weekStartDate, List.of()).stream()
				.mapToLong(allocation -> normalizeAmount(allocation.getFinalAmount()))
				.sum();
			long budgetWeekPlanSavedAmount = sumPlanSavedAmount(budgetWeek);
			long finalFreeAmount = plannedIncomeAmount
				- plannedEssentialAmount
				- finalSavingAmount
				- normalizeAmount(budgetWeek.getRolloverInAmount())
				+ normalizeAmount(budgetWeek.getManualAdjustmentAmount())
				- budgetWeekPlanSavedAmount;

			budgetWeek.setPlannedIncomeAmount(plannedIncomeAmount);
			budgetWeek.setPlannedEssentialAmount(plannedEssentialAmount);
			budgetWeek.setPlannedSavingAmount(plannedSavingAmount);
			budgetWeek.setFinalFreeAmount(finalFreeAmount);
			budgetWeeks.add(budgetWeek);
		}

		return budgetWeekRepository.saveAll(budgetWeeks);
	}

	private List<PlanWeekAllocation> flattenPlanAllocations(
		List<WeekFrame> weekFrames,
		Map<LocalDate, List<PlanWeekAllocation>> planAllocationsByWeekStartDate
	) {
		List<PlanWeekAllocation> planAllocations = new ArrayList<>();
		for (WeekFrame weekFrame : weekFrames) {
			planAllocations.addAll(planAllocationsByWeekStartDate.getOrDefault(weekFrame.startDate(), List.of()));
		}
		return planAllocations;
	}

	private void updateShortageSummary(Map<Plan, PlanShortageSummary> shortageSummaries, Plan plan, long deduction) {
		PlanShortageSummary currentSummary = shortageSummaries.get(plan);
		if (currentSummary == null) {
			long baseWeeklyAmount = calculateBaseWeeklyAmount(normalizeAmount(plan.getTargetAmount()), plan.getTargetWeeks());
			currentSummary = new PlanShortageSummary(plan, baseWeeklyAmount, 0L, 0);
		}

		long deductedAmount = currentSummary.deductedAmount() + deduction;
		int extraWeeks = currentSummary.baseWeeklyAmount() > 0L
			? (int)((deductedAmount + currentSummary.baseWeeklyAmount() - 1L) / currentSummary.baseWeeklyAmount())
			: 0;
		shortageSummaries.put(plan, new PlanShortageSummary(plan, currentSummary.baseWeeklyAmount(), deductedAmount, extraWeeks));
	}

	private Comparator<PlanWeekAllocation> planShortageOrder() {
		return Comparator
			.comparingInt((PlanWeekAllocation allocation) -> allocation.getPlan().getPriority())
			.reversed()
			.thenComparing(allocation -> allocation.getPlan().getId(), Comparator.nullsLast(Integer::compareTo));
	}

	private Map<LocalDate, MonthlyEssentialTotal> aggregateEssentialTotals(List<EssentialExpenseMonth> essentials) {
		Map<LocalDate, MonthlyEssentialTotal> totalsByMonth = new HashMap<>();
		for (EssentialExpenseMonth essential : essentials) {
			LocalDate monthStartDate = monthStart(essential.getBudgetMonth().getBaseDate());
			MonthlyEssentialTotal currentTotal = totalsByMonth.getOrDefault(monthStartDate, MonthlyEssentialTotal.ZERO);
			long amount = normalizeAmount(essential.getAmount());
			MonthlyEssentialTotal nextTotal = parseExpenseScope(essential.getExpenseScope()) == ExpenseScope.FIXED
				? new MonthlyEssentialTotal(currentTotal.fixedAmount() + amount, currentTotal.variableAmount())
				: new MonthlyEssentialTotal(currentTotal.fixedAmount(), currentTotal.variableAmount() + amount);
			totalsByMonth.put(monthStartDate, nextTotal);
		}
		return totalsByMonth;
	}

	private Map<LocalDate, Long> distributeByDenominator(long totalAmount, long denominator, List<WeightedWeekFrame> slices) {
		Map<LocalDate, Long> distributedAmounts = new HashMap<>();
		if (totalAmount <= 0L || denominator <= 0L || slices.isEmpty()) {
			return distributedAmounts;
		}

		List<DistributionCandidate> candidates = new ArrayList<>();
		long floorSum = 0L;
		long numeratorSum = 0L;

		for (WeightedWeekFrame slice : slices) {
			long numerator = totalAmount * slice.weight();
			long floorAmount = numerator / denominator;
			long remainder = numerator % denominator;
			floorSum += floorAmount;
			numeratorSum += numerator;
			candidates.add(new DistributionCandidate(slice.weekStartDate(), floorAmount, remainder));
		}

		long targetTotal = (numeratorSum + (denominator / 2L)) / denominator;
		long remainderUnits = targetTotal - floorSum;
		candidates.sort(Comparator.comparingLong(DistributionCandidate::remainder).reversed()
			.thenComparing(DistributionCandidate::weekStartDate));

		for (int index = 0; index < candidates.size(); index++) {
			DistributionCandidate candidate = candidates.get(index);
			long distributedAmount = candidate.floorAmount() + (index < remainderUnits ? 1L : 0L);
			distributedAmounts.put(candidate.weekStartDate(), distributedAmount);
		}

		return distributedAmounts;
	}

	private Map<LocalDate, Long> initializeWeeklyTotals(List<WeekFrame> weekFrames) {
		Map<LocalDate, Long> totalsByWeekStartDate = new LinkedHashMap<>();
		for (WeekFrame weekFrame : weekFrames) {
			totalsByWeekStartDate.put(weekFrame.startDate(), 0L);
		}
		return totalsByWeekStartDate;
	}

	private void mergeWeeklyTotals(Map<LocalDate, Long> target, Map<LocalDate, Long> source) {
		for (Map.Entry<LocalDate, Long> entry : source.entrySet()) {
			target.merge(entry.getKey(), entry.getValue(), Long::sum);
		}
	}

	private List<WeightedWeekFrame> slicesForPeriod(LocalDate periodStartDate, LocalDate periodEndDate, List<WeekFrame> weekFrames) {
		List<WeightedWeekFrame> slices = new ArrayList<>();
		for (WeekFrame weekFrame : weekFrames) {
			LocalDate sliceStartDate = maxDate(periodStartDate, weekFrame.startDate());
			LocalDate sliceEndDate = minDate(periodEndDate, weekFrame.endDate());
			if (sliceEndDate.isBefore(sliceStartDate)) {
				continue;
			}

			long weight = ChronoUnit.DAYS.between(sliceStartDate, sliceEndDate) + 1L;
			slices.add(new WeightedWeekFrame(weekFrame.startDate(), weight));
		}
		return slices;
	}

	private List<WeekFrame> buildWeekFrames(LocalDate serviceStartDate, LocalDate allocationEndDate) {
		List<WeekFrame> weekFrames = new ArrayList<>();
		LocalDate cursor = serviceStartDate;

		while (!cursor.isAfter(allocationEndDate)) {
			LocalDate weekEndDate = minDate(cursor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)), allocationEndDate);
			int periodDays = (int)(ChronoUnit.DAYS.between(cursor, weekEndDate) + 1L);
			boolean partial = cursor.getDayOfWeek() != DayOfWeek.MONDAY || weekEndDate.getDayOfWeek() != DayOfWeek.SUNDAY;
			weekFrames.add(new WeekFrame(cursor, weekEndDate, periodDays, partial));
			cursor = weekEndDate.plusDays(1L);
		}

		return weekFrames;
	}

	private long calculateBaseWeeklyAmount(long remainingAmount, Integer targetWeeks) {
		if (remainingAmount <= 0L || targetWeeks == null || targetWeeks <= 0) {
			return 0L;
		}
		return remainingAmount / targetWeeks;
	}

	private long elapsedPlanDays(LocalDate planStartDate, LocalDate planEndDate, LocalDate targetDate) {
		if (planStartDate == null || planEndDate == null || targetDate == null || targetDate.isBefore(planStartDate)) {
			return 0L;
		}
		LocalDate effectiveEndDate = minDate(targetDate, planEndDate);
		if (effectiveEndDate.isBefore(planStartDate)) {
			return 0L;
		}
		return ChronoUnit.DAYS.between(planStartDate, effectiveEndDate) + 1L;
	}

	private long proratedAmount(long totalAmount, long denominator, long numerator) {
		if (totalAmount <= 0L || denominator <= 0L || numerator <= 0L) {
			return 0L;
		}
		long cappedNumerator = Math.min(numerator, denominator);
		return (totalAmount * cappedNumerator + (denominator / 2L)) / denominator;
	}

	private ExpenseScope parseExpenseScope(String expenseScope) {
		if (expenseScope == null || expenseScope.isBlank()) {
			return ExpenseScope.VARIABLE;
		}
		return ExpenseScope.valueOf(expenseScope.trim().toUpperCase());
	}

	private long sumPlanSavedAmount(BudgetWeek budgetWeek) {
		return budgetAdjustmentRepository.findByBudgetWeekOrderByCreatedAtDesc(budgetWeek).stream()
			.filter(adjustment -> adjustment.getAdjustmentType() == BudgetAdjustmentType.PLAN_SAVED_ADD)
			.mapToLong(adjustment -> normalizeAmount(adjustment.getAdjustmentAmount()))
			.sum();
	}

	private long normalizeAmount(Long amount) {
		return amount == null ? 0L : Math.max(0L, amount);
	}

	private LocalDate monthStart(LocalDate date) {
		return date.withDayOfMonth(1);
	}

	private LocalDate defaultDate(LocalDate date, LocalDate defaultDate) {
		return date != null ? date : defaultDate;
	}

	private LocalDate maxDate(LocalDate left, LocalDate right) {
		return left.isAfter(right) ? left : right;
	}

	private LocalDate minDate(LocalDate left, LocalDate right) {
		return left.isBefore(right) ? left : right;
	}

	public record AllocationResult(
		List<BudgetMonth> budgetMonths,
		List<BudgetWeek> budgetWeeks,
		List<EssentialWeekAllocation> essentialAllocations,
		List<PlanWeekAllocation> planAllocations,
		List<PlanShortageSummary> planShortageSummaries
	) {
	}

	public record PlanShortageSummary(
		Plan plan,
		long baseWeeklyAmount,
		long deductedAmount,
		int extraWeeks
	) {
	}

	private record PlanAllocationBundle(
		Map<LocalDate, List<PlanWeekAllocation>> allocationsByWeekStartDate,
		Map<Plan, PlanShortageSummary> shortageSummaries
	) {
	}

	private record WeekFrame(
		LocalDate startDate,
		LocalDate endDate,
		int periodDays,
		boolean partial
	) {
	}

	private record WeightedWeekFrame(
		LocalDate weekStartDate,
		long weight
	) {
	}

	private record DistributionCandidate(
		LocalDate weekStartDate,
		long floorAmount,
		long remainder
	) {
	}

	private record PriorPlanAllocationState(long finalAmount, boolean exists) {
		private static final PriorPlanAllocationState EMPTY = new PriorPlanAllocationState(0L, false);
	}

	private record MonthlyEssentialTotal(long fixedAmount, long variableAmount) {

		private static final MonthlyEssentialTotal ZERO = new MonthlyEssentialTotal(0L, 0L);
	}

	private record ExistingEssentialAllocationState(long actualSpentAmount, String note) {
	}

	private record EssentialAllocationKey(Integer essentialExpenseMonthId, LocalDate weekStartDate) {

		private static EssentialAllocationKey from(LocalDate weekStartDate, EssentialExpenseMonth essentialExpenseMonth) {
			return new EssentialAllocationKey(essentialExpenseMonth.getId(), weekStartDate);
		}

		private static EssentialAllocationKey from(LocalDate weekStartDate, EssentialWeekAllocation allocation) {
			return new EssentialAllocationKey(allocation.getEssentialExpenseMonth().getId(), weekStartDate);
		}
	}

	private record PlanAllocationKey(Integer planId, LocalDate weekStartDate) {
	}

	private record ExistingPlanAllocationState(Long actualAmount, String note) {
	}
}
