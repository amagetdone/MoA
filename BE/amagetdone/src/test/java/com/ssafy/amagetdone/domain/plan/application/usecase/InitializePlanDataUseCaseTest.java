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

	@Mock
	private UserRepository userRepository;

	@Mock
	private PlanRepository planRepository;

	@Test
	void executeCreatesAllSeedPlansWhenMissing() {
		User user = User.builder().id(13).build();
		InitializePlanDataUseCase useCase = new InitializePlanDataUseCase(userRepository, planRepository);
		given(userRepository.findById(13)).willReturn(Optional.of(user));
		given(planRepository.existsByUserIdAndTitleAndDeletedFalse(any(), any())).willReturn(false);
		given(planRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		AccountAdminDto.PlanDataInitResponse response = useCase.execute(new AccountAdminDto.PlanDataInitRequest(13));

		ArgumentCaptor<List<Plan>> captor = listCaptor();
		verify(planRepository).saveAll(captor.capture());
		assertThat(response.targetType()).isEqualTo("plan");
		assertThat(response.userId()).isEqualTo(13);
		assertThat(response.createdCount()).isEqualTo(3);
		assertThat(response.skippedCount()).isEqualTo(0);
		assertThat(response.createdTitles()).containsExactly("맥북 프로 구매", "여행 자금 모으기", "자격증 교재 구매");
		assertThat(response.skippedTitles()).isEmpty();
		assertThat(captor.getValue())
			.extracting(Plan::getTitle, Plan::getSavedAmount, Plan::getTargetAmount, Plan::getStatus)
			.containsExactly(
				Tuple.tuple("맥북 프로 구매", 900_000L, 900_000L, PlanStatus.COMPLETED),
				Tuple.tuple("여행 자금 모으기", 408_791L, 600_000L, PlanStatus.ACTIVE),
				Tuple.tuple("자격증 교재 구매", 102_198L, 300_000L, PlanStatus.ACTIVE)
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
		given(planRepository.existsByUserIdAndTitleAndDeletedFalse(13, "맥북 프로 구매")).willReturn(true);
		given(planRepository.existsByUserIdAndTitleAndDeletedFalse(13, "여행 자금 모으기")).willReturn(false);
		given(planRepository.existsByUserIdAndTitleAndDeletedFalse(13, "자격증 교재 구매")).willReturn(true);
		given(planRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		AccountAdminDto.PlanDataInitResponse response = useCase.execute(new AccountAdminDto.PlanDataInitRequest(13));

		ArgumentCaptor<List<Plan>> captor = listCaptor();
		verify(planRepository).saveAll(captor.capture());
		assertThat(captor.getValue()).extracting(Plan::getTitle).containsExactly("여행 자금 모으기");
		assertThat(response.createdCount()).isEqualTo(1);
		assertThat(response.skippedCount()).isEqualTo(2);
		assertThat(response.createdTitles()).containsExactly("여행 자금 모으기");
		assertThat(response.skippedTitles()).containsExactly("맥북 프로 구매", "자격증 교재 구매");
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

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<List<Plan>> listCaptor() {
		return (ArgumentCaptor<List<Plan>>) (ArgumentCaptor<?>)ArgumentCaptor.forClass(List.class);
	}
}
