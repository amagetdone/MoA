package com.ssafy.amagetdone.domain.account.api.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ssafy.amagetdone.domain.plan.model.vo.PlanStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public final class AccountAdminDto {

	private AccountAdminDto() {
	}

	public record ProductCreateRequest(
		String bankCode,
		String accountName,
		String accountDescription
	) {
	}

	public record ProductResponse(
		String accountTypeUniqueNo,
		String bankCode,
		String bankName,
		String accountTypeCode,
		String accountTypeName,
		String accountName,
		String accountDescription,
		String accountType
	) {
	}

	public record DataInitRequest(
		String apiKey,
		String userApiKey
	) {
	}

	public record AccountCreateRequest(
		Integer userId,
		String accountTypeUniqueNo
	) {
	}

	public record AccountCreateResponse(
		String bankCode,
		String accountNo,
		String currency,
		String currencyName
	) {
	}

	@Schema(name = "AccountTransactionInitRequest")
	public record AccountTransactionInitRequest(
		@Schema(
			description = "우리 시스템에 저장된 계좌번호",
			example = "0016174648358792"
		)
		String accountNo
	) {
	}

	@Schema(name = "CardTransactionInitRequest")
	public record CardTransactionInitRequest(
		@Schema(
			description = "우리 시스템에 저장된 카드번호",
			example = "5241111122223333"
		)
		String cardNo
	) {
	}

	@Schema(name = "TransactionInitResponse")
	public record TransactionInitResponse(
		@Schema(description = "대상 유형", example = "account")
		String targetType,
		@Schema(description = "대상 번호", example = "0016174648358792")
		String targetNo,
		@Schema(description = "생성된 거래 건수", example = "109")
		int transactionCount
	) {
	}

	@Schema(name = "PlanDataInitRequest")
	public record PlanDataInitRequest(
		@Schema(description = "초기화 대상 사용자 ID", example = "13")
		Integer userId
	) {
	}

	@Schema(name = "PlanDataInitResponse")
	public record PlanDataInitResponse(
		@Schema(description = "초기화 대상 타입", example = "plan")
		String targetType,
		@Schema(description = "초기화 대상 사용자 ID", example = "13")
		Integer userId,
		@Schema(description = "새로 생성된 플랜 수", example = "2")
		int createdCount,
		@Schema(description = "이미 존재해서 건너뛴 플랜 수", example = "1")
		int skippedCount,
		@Schema(description = "새로 생성된 플랜 제목 목록")
		List<String> createdTitles,
		@Schema(description = "건너뛴 플랜 제목 목록")
		List<String> skippedTitles
	) {
	}

	@Schema(
		name = "DemoPlanSeedRequest",
		example = """
			{
			  "userId": 12,
			  "plans": [
			    {
			      "title": "유럽 여행",
			      "description": "유럽 배낭 여행을 위한 저축",
			      "targetAmount": 1000000,
			      "priority": 1,
			      "status": "ACTIVE",
			      "startDate": "2026-03-09",
			      "endDate": "2026-04-26",
			      "originalEndDate": "2026-04-26",
			      "weeks": [
			        { "weekNumber": 1, "plannedAmount": 125000, "actualAmount": 125000 },
			        { "weekNumber": 2, "plannedAmount": 125000, "actualAmount": 125000 },
			        { "weekNumber": 3, "plannedAmount": 125000, "actualAmount": 125000 },
			        { "weekNumber": 4, "plannedAmount": 125000, "actualAmount": 62500 },
			        { "weekNumber": 5, "plannedAmount": 125000, "actualAmount": 0 },
			        { "weekNumber": 6, "plannedAmount": 125000, "actualAmount": 0 },
			        { "weekNumber": 7, "plannedAmount": 125000, "actualAmount": 0 }
			      ]
			    }
			  ]
			}
			"""
	)
	public record DemoPlanSeedRequest(
		@Schema(description = "데모 플랜을 세팅할 사용자 ID", example = "12")
		@NotNull Integer userId,
		@Schema(description = "사용자의 기존 플랜/저축 이력을 덮어쓸 데모 플랜 목록")
		@NotEmpty List<@Valid DemoPlanSeedPlanRequest> plans
	) {
	}

	@Schema(name = "DemoPlanSeedPlanRequest")
	public record DemoPlanSeedPlanRequest(
		@NotBlank String title,
		String description,
		@NotNull @Positive Long targetAmount,
		@NotNull @Positive Integer priority,
		@NotNull PlanStatus status,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate,
		LocalDate originalEndDate,
		@NotEmpty List<@Valid DemoPlanSeedWeekRequest> weeks
	) {

		@JsonIgnore
		@Schema(hidden = true)
		@AssertTrue(message = "startDate must be on or before endDate")
		public boolean isDateRangeValid() {
			if (startDate == null || endDate == null) {
				return true;
			}
			return !startDate.isAfter(endDate);
		}

		@JsonIgnore
		@Schema(hidden = true)
		@AssertTrue(message = "originalEndDate must be on or before endDate")
		public boolean isOriginalEndDateValid() {
			if (originalEndDate == null || endDate == null) {
				return true;
			}
			return !originalEndDate.isAfter(endDate);
		}
	}

	@Schema(name = "DemoPlanSeedWeekRequest")
	public record DemoPlanSeedWeekRequest(
		@NotNull @Positive Integer weekNumber,
		@NotNull @PositiveOrZero Long plannedAmount,
		@PositiveOrZero Long finalAmount,
		@NotNull @PositiveOrZero Long actualAmount
	) {
	}

	@Schema(name = "DemoPlanSeedResponse")
	public record DemoPlanSeedResponse(
		String targetType,
		Integer userId,
		int replacedPlanCount,
		int seededPlanCount,
		int seededWeekCount,
		long seededActualAmount,
		List<DemoPlanSeedPlanResult> plans
	) {
	}

	@Schema(name = "DemoPlanSeedPlanResult")
	public record DemoPlanSeedPlanResult(
		Integer planId,
		String title,
		PlanStatus status,
		Long savedAmount,
		Long targetAmount,
		LocalDate startDate,
		LocalDate endDate,
		LocalDate originalEndDate,
		int totalWeeks
	) {
	}

	@Schema(name = "AccountPaymentRequest")
	public record AccountPaymentRequest(
		@Schema(description = "출금 계좌번호", example = "0016174648358792")
		String accountNo,
		@Schema(description = "입금 상대 계좌번호", example = "0010000000000001")
		String counterpartyAccountNo,
		@Schema(description = "이체 금액", example = "10000")
		Long amount,
		@Schema(description = "출금 거래 요약", example = "테스트 출금")
		String withdrawalSummary,
		@Schema(description = "입금 거래 요약", example = "테스트 입금")
		String depositSummary
	) {
	}

	@Schema(name = "CardPaymentRequest")
	public record CardPaymentRequest(
		@Schema(description = "결제 카드번호", example = "5241111122223333")
		String cardNo,
		@Schema(description = "가맹점 ID", example = "101001")
		Long merchantId,
		@Schema(description = "결제 금액", example = "28900")
		Long amount
	) {
	}

	@Schema(name = "CardPaymentCancelRequest")
	public record CardPaymentCancelRequest(
		@Schema(description = "결제 카드번호", example = "5241111122223333")
		String cardNo,
		@Schema(description = "취소할 거래 고유번호", example = "88")
		Long transactionUniqueNo
	) {
	}

	@Schema(name = "PaymentResponse")
	public record PaymentResponse(
		@Schema(description = "대상 유형", example = "card")
		String targetType,
		@Schema(description = "대상 번호", example = "5241111122223333")
		String targetNo,
		@Schema(description = "거래 고유번호", example = "88")
		Long transactionUniqueNo,
		@Schema(description = "거래 일자", example = "20260324")
		String transactionDate,
		@Schema(description = "거래 시각", example = "092700")
		String transactionTime,
		@Schema(description = "거래 금액", example = "28900")
		Long amount,
		@Schema(description = "거래 상태", example = "BILLED")
		String status
	) {
	}
}
