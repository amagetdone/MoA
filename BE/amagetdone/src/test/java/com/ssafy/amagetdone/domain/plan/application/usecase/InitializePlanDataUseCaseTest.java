package com.ssafy.amagetdone.domain.plan.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.member.exception.MemberErrorCode;
import com.ssafy.amagetdone.domain.member.exception.MemberException;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.member.model.repository.UserRepository;
import com.ssafy.amagetdone.domain.plan.exception.PlanErrorCode;
import com.ssafy.amagetdone.domain.plan.exception.PlanException;
import com.ssafy.amagetdone.domain.plan.model.entity.Plan;
import com.ssafy.amagetdone.domain.plan.model.repository.PlanRepository;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanAiPipelineStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;
import com.ssafy.amagetdone.domain.plan.model.vo.PyanTaskStatus;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitializePlanDataUseCaseTest {

	private static final String COMPLETED_SEED_TITLE = "\uB9E5\uBD81 \uD504\uB85C \uAD6C\uB9E4";
	private static final String ACTIVE_SEED_TITLE_ONE = "\uC5EC\uD589 \uC790\uAE08 \uBAA8\uC73C\uAE30";
	private static final String ACTIVE_SEED_TITLE_TWO = "\uC790\uACA9\uC99D \uAD50\uC7AC \uAD6C\uB9E4";

	@Mock
	private UserRepository userRepository;

	@Mock
	private PlanRepository planRepository;

	@Test
	void executeCreatesAllSeedPlansWhenMissing() {
		User user = User.builder().id(13).build();
		InitializePlanDataUseCase useCase = new InitializePlanDataUseCase(userRepository, planRepository);
		given(userRepository.findById(13)).willReturn(Optional.of(user));
		given(planRepository.findByUserIdAndTitleAndDeletedFalse(any(), any())).willReturn(Optional.empty());
		given(planRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		AccountAdminDto.PlanDataInitResponse response = useCase.execute(new AccountAdminDto.PlanDataInitRequest(13));

		ArgumentCaptor<List<Plan>> captor = listCaptor();
		verify(planRepository).saveAll(captor.capture());
		assertThat(response.targetType()).isEqualTo("plan");
		assertThat(response.userId()).isEqualTo(13);
		assertThat(response.createdCount()).isEqualTo(3);
		assertThat(response.skippedCount()).isEqualTo(0);
		assertThat(response.createdTitles()).containsExactly(
			COMPLETED_SEED_TITLE,
			ACTIVE_SEED_TITLE_ONE,
			ACTIVE_SEED_TITLE_TWO
		);
		assertThat(response.skippedTitles()).isEmpty();
		assertThat(captor.getValue())
			.extracting(Plan::getTitle, Plan::getSavedAmount, Plan::getTargetAmount, Plan::getStatus)
			.containsExactly(
				Tuple.tuple(COMPLETED_SEED_TITLE, 900_000L, 900_000L, PlanStatus.COMPLETED),
				Tuple.tuple(ACTIVE_SEED_TITLE_ONE, 408_791L, 600_000L, PlanStatus.ACTIVE),
				Tuple.tuple(ACTIVE_SEED_TITLE_TWO, 102_198L, 300_000L, PlanStatus.ACTIVE)
			);
		assertThat(captor.getValue())
			.extracting(Plan::getStartDate, Plan::getEndDate, Plan::getPriority)
			.containsExactly(
				Tuple.tuple(LocalDate.of(2025, 12, 27), LocalDate.of(2026, 3, 27), 1),
				Tuple.tuple(LocalDate.of(2026, 1, 27), LocalDate.of(2026, 4, 27), 2),
				Tuple.tuple(LocalDate.of(2026, 2, 27), LocalDate.of(2026, 5, 27), 3)
			);
		assertThat(captor.getValue())
			.extracting(Plan::isAutoAllocateEnabled, Plan::getAiPipelineStatus, Plan::getPyanStatus)
			.containsOnly(Tuple.tuple(true, PlanAiPipelineStatus.SUCCEEDED, PyanTaskStatus.READY));
	}

	@Test
	void executeSkipsExistingTitlesAndSavesOnlyMissingSeeds() {
		User user = User.builder().id(13).build();
		InitializePlanDataUseCase useCase = new InitializePlanDataUseCase(userRepository, planRepository);
		given(userRepository.findById(13)).willReturn(Optional.of(user));
		given(planRepository.findByUserIdAndTitleAndDeletedFalse(13, COMPLETED_SEED_TITLE)).willReturn(
			Optional.of(completedSeedPlan(user))
		);
		given(planRepository.findByUserIdAndTitleAndDeletedFalse(13, ACTIVE_SEED_TITLE_ONE)).willReturn(Optional.empty());
		given(planRepository.findByUserIdAndTitleAndDeletedFalse(13, ACTIVE_SEED_TITLE_TWO)).willReturn(
			Optional.of(activeSeedPlan(user, ACTIVE_SEED_TITLE_TWO, 300_000L, 102_198L, 3, LocalDate.of(2026, 2, 27), LocalDate.of(2026, 5, 27)))
		);
		given(planRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		AccountAdminDto.PlanDataInitResponse response = useCase.execute(new AccountAdminDto.PlanDataInitRequest(13));

		ArgumentCaptor<List<Plan>> captor = listCaptor();
		verify(planRepository).saveAll(captor.capture());
		assertThat(captor.getValue()).extracting(Plan::getTitle).containsExactly(ACTIVE_SEED_TITLE_ONE);
		assertThat(response.createdCount()).isEqualTo(1);
		assertThat(response.skippedCount()).isEqualTo(2);
		assertThat(response.createdTitles()).containsExactly(ACTIVE_SEED_TITLE_ONE);
		assertThat(response.skippedTitles()).containsExactly(COMPLETED_SEED_TITLE, ACTIVE_SEED_TITLE_TWO);
	}

	@Test
	void executeBackfillsCompletedSeedWhenExistingGoalWasOldActiveSeed() {
		User user = User.builder().id(13).build();
		InitializePlanDataUseCase useCase = new InitializePlanDataUseCase(userRepository, planRepository);
		Plan existingCompletedSeed = completedSeedPlan(user);
		existingCompletedSeed.setStatus(PlanStatus.ACTIVE);
		existingCompletedSeed.setSavedAmount(250_000L);
		existingCompletedSeed.setTargetAmount(700_000L);

		given(userRepository.findById(13)).willReturn(Optional.of(user));
		given(planRepository.findByUserIdAndTitleAndDeletedFalse(13, COMPLETED_SEED_TITLE)).willReturn(
			Optional.of(existingCompletedSeed)
		);
		given(planRepository.findByUserIdAndTitleAndDeletedFalse(13, ACTIVE_SEED_TITLE_ONE)).willReturn(Optional.empty());
		given(planRepository.findByUserIdAndTitleAndDeletedFalse(13, ACTIVE_SEED_TITLE_TWO)).willReturn(Optional.empty());
		given(planRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		AccountAdminDto.PlanDataInitResponse response = useCase.execute(new AccountAdminDto.PlanDataInitRequest(13));

		ArgumentCaptor<List<Plan>> captor = listCaptor();
		verify(planRepository).saveAll(captor.capture());
		assertThat(existingCompletedSeed.getStatus()).isEqualTo(PlanStatus.COMPLETED);
		assertThat(existingCompletedSeed.getSavedAmount()).isEqualTo(900_000L);
		assertThat(existingCompletedSeed.getTargetAmount()).isEqualTo(900_000L);
		assertThat(captor.getValue())
			.extracting(Plan::getTitle, Plan::getStatus, Plan::getSavedAmount, Plan::getTargetAmount)
			.contains(Tuple.tuple(COMPLETED_SEED_TITLE, PlanStatus.COMPLETED, 900_000L, 900_000L));
		assertThat(response.createdCount()).isEqualTo(2);
		assertThat(response.skippedCount()).isEqualTo(1);
		assertThat(response.skippedTitles()).containsExactly(COMPLETED_SEED_TITLE);
	}

	@Test
	void executeRejectsMissingUserId() {
		InitializePlanDataUseCase useCase = new InitializePlanDataUseCase(userRepository, planRepository);

		assertThatThrownBy(() -> useCase.execute(new AccountAdminDto.PlanDataInitRequest(null)))
			.isInstanceOf(PlanException.class)
			.extracting(exception -> ((PlanException)exception).getErrorCode())
			.isEqualTo(PlanErrorCode.INVALID_PLAN_DATA_INIT_REQUEST);
		verify(userRepository, never()).findById(any());
	}

	@Test
	void executeRejectsUnknownUser() {
		InitializePlanDataUseCase useCase = new InitializePlanDataUseCase(userRepository, planRepository);
		given(userRepository.findById(13)).willReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.execute(new AccountAdminDto.PlanDataInitRequest(13)))
			.isInstanceOf(MemberException.class)
			.extracting(exception -> ((MemberException)exception).getErrorCode())
			.isEqualTo(MemberErrorCode.USER_NOT_FOUND);
	}

	private Plan completedSeedPlan(User user) {
		Plan plan = Plan.create(
			user,
			COMPLETED_SEED_TITLE,
			null,
			900_000L,
			LocalDate.of(2025, 12, 27),
			LocalDate.of(2026, 3, 27),
			1
		);
		plan.setSavedAmount(900_000L);
		plan.setStatus(PlanStatus.COMPLETED);
		return plan;
	}

	private Plan activeSeedPlan(
		User user,
		String title,
		long targetAmount,
		long savedAmount,
		int priority,
		LocalDate startDate,
		LocalDate endDate
	) {
		Plan plan = Plan.create(user, title, null, targetAmount, startDate, endDate, priority);
		plan.setSavedAmount(savedAmount);
		plan.setStatus(PlanStatus.ACTIVE);
		return plan;
	}

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<List<Plan>> listCaptor() {
		return (ArgumentCaptor<List<Plan>>) (ArgumentCaptor<?>)ArgumentCaptor.forClass(List.class);
	}
}
