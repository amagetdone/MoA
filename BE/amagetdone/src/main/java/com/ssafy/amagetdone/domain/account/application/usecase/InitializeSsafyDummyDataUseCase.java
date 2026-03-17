package com.ssafy.amagetdone.domain.account.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminCreditCardApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminDemandDepositApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.AdminCreditCardDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.account.model.repository.MerchantRepository;
import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.domain.transaction.model.entity.AccountTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.AccountTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitializeSsafyDummyDataUseCase {

	private static final String PRIMARY_ACCOUNT_PRODUCT_ID = "001-1-ee26d1d80a4542";
	private static final String CARD_PRODUCT_ID = "1001-eebb34baf8204cf";
	private static final String WITHDRAWAL_DATE = "3";
	private static final int DEFAULT_CATEGORY_ID = 4;
	private static final int LOOKBACK_COMPLETED_MONTHS = 3;

	private final FindAccountUserByUserKeyUseCase findAccountUserByUserKeyUseCase;
	private final AccountRepository accountRepository;
	private final CardRepository cardRepository;
	private final CategoryRepository categoryRepository;
	private final MerchantRepository merchantRepository;
	private final AccountTransactionOverrideRepository accountTransactionOverrideRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final SsafyAdminDemandDepositApiClient ssafyAdminDemandDepositApiClient;
	private final SsafyAdminCreditCardApiClient ssafyAdminCreditCardApiClient;

	private Clock clock = Clock.systemDefaultZone();

	public InitializeSsafyDummyDataUseCase(
		FindAccountUserByUserKeyUseCase findAccountUserByUserKeyUseCase,
		AccountRepository accountRepository,
		CardRepository cardRepository,
		CategoryRepository categoryRepository,
		MerchantRepository merchantRepository,
		AccountTransactionOverrideRepository accountTransactionOverrideRepository,
		CardTransactionOverrideRepository cardTransactionOverrideRepository,
		SsafyAdminDemandDepositApiClient ssafyAdminDemandDepositApiClient,
		SsafyAdminCreditCardApiClient ssafyAdminCreditCardApiClient
	) {
		this.findAccountUserByUserKeyUseCase = findAccountUserByUserKeyUseCase;
		this.accountRepository = accountRepository;
		this.cardRepository = cardRepository;
		this.categoryRepository = categoryRepository;
		this.merchantRepository = merchantRepository;
		this.accountTransactionOverrideRepository = accountTransactionOverrideRepository;
		this.cardTransactionOverrideRepository = cardTransactionOverrideRepository;
		this.ssafyAdminDemandDepositApiClient = ssafyAdminDemandDepositApiClient;
		this.ssafyAdminCreditCardApiClient = ssafyAdminCreditCardApiClient;
	}

	@Transactional
	public AccountAdminDto.DataInitResponse execute(AccountAdminDto.DataInitRequest request) {
		validateRequest(request);
		User user = findAccountUserByUserKeyUseCase.execute(request.userApiKey());
		Category defaultCategory = categoryRepository.findById(DEFAULT_CATEGORY_ID)
			.orElseThrow(() -> new AccountException(AccountErrorCode.ACCOUNT_ERROR));

		SsafyAdminDemandDepositApiClient.AccountCreationResult primaryAccountResult =
			ssafyAdminDemandDepositApiClient.createAccount(request.apiKey(), request.userApiKey(), PRIMARY_ACCOUNT_PRODUCT_ID);
		Account primaryAccount = saveAccount(user, primaryAccountResult, "모아 급여 계좌", "분석용 주거래 계좌");

		SsafyAdminDemandDepositApiClient.AccountCreationResult helperAccountResult =
			ssafyAdminDemandDepositApiClient.createHelperAccount(request.apiKey(), request.userApiKey(), PRIMARY_ACCOUNT_PRODUCT_ID);
		Account helperAccount = saveAccount(user, helperAccountResult, "모아 보조 계좌", "내부이체 검증용 보조 계좌");

		List<YearMonth> targetMonths = resolveTargetMonths();

		List<AccountTransactionSeed> accountSeeds = buildAccountSeeds(targetMonths, helperAccount.getAccountNo());
		accountTransactionOverrideRepository.saveAll(mapAccountTransactions(primaryAccount, accountSeeds, defaultCategory));

		AdminCreditCardDto.CreatedCard createdCard = ssafyAdminCreditCardApiClient.createCard(
			request.apiKey(),
			request.userApiKey(),
			CARD_PRODUCT_ID,
			primaryAccount.getAccountNo(),
			WITHDRAWAL_DATE
		);
		Card card = saveCard(primaryAccount, createdCard);

		List<CardTransactionSeed> cardSeeds = buildCardSeeds(targetMonths);
		cardTransactionOverrideRepository.saveAll(mapCardTransactions(card, cardSeeds, defaultCategory));

		return new AccountAdminDto.DataInitResponse(
			primaryAccount.getAccountNo(),
			createdCard.cardNo(),
			createdCard.cardUniqueNo(),
			accountSeeds.size(),
			cardSeeds.size()
		);
	}

	private Account saveAccount(
		User user,
		SsafyAdminDemandDepositApiClient.AccountCreationResult result,
		String accountName,
		String accountDescription
	) {
		Account account = accountRepository.findByUserIdAndAccountNo(user.getId(), result.accountNo())
			.orElseGet(() -> Account.create(
				user,
				result.accountNo(),
				PRIMARY_ACCOUNT_PRODUCT_ID,
				accountName,
				accountDescription,
				result.bankCode(),
				result.bankCode(),
				result.currency()
			));
		account.sync(
			PRIMARY_ACCOUNT_PRODUCT_ID,
			accountName,
			accountDescription,
			result.bankCode(),
			result.bankCode(),
			result.currency()
		);
		return accountRepository.save(account);
	}

	private Card saveCard(Account withdrawalAccount, AdminCreditCardDto.CreatedCard createdCard) {
		Card card = Card.builder()
			.cardUniqueNo(toLocalCardUniqueNo(createdCard.cardUniqueNo()))
			.withdrawalAccount(withdrawalAccount)
			.cvc(createdCard.cvc())
			.cardNo(createdCard.cardNo())
			.name(createdCard.cardName())
			.issuerCode(createdCard.cardIssuerCode())
			.cardIssuerName(createdCard.cardIssuerName())
			.cardExpiryDate(LocalDate.parse(createdCard.cardExpiryDate(), java.time.format.DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay())
			.cardDescription(createdCard.cardDescription() == null ? "" : createdCard.cardDescription())
			.withdrawalDate(createdCard.withdrawalDate())
			.baselinePerformance(createdCard.baselinePerformance())
			.maxBenefitLimit(createdCard.maxBenefitLimit())
			.build();
		return cardRepository.save(card);
	}

	private List<YearMonth> resolveTargetMonths() {
		YearMonth latestCompletedMonth = YearMonth.from(LocalDate.now(clock)).minusMonths(1);
		YearMonth oldestMonth = latestCompletedMonth.minusMonths(LOOKBACK_COMPLETED_MONTHS - 1L);
		List<YearMonth> months = new ArrayList<>(LOOKBACK_COMPLETED_MONTHS);
		for (int offset = 0; offset < LOOKBACK_COMPLETED_MONTHS; offset++) {
			months.add(oldestMonth.plusMonths(offset));
		}
		return months;
	}

	private List<AccountTransactionSeed> buildAccountSeeds(List<YearMonth> targetMonths, String helperAccountNo) {
		List<AccountTransactionSeed> seeds = new ArrayList<>();
		long sequence = 1_000L;
		long[] salaryAmounts = {3_200_000L, 3_250_000L, 3_180_000L};
		long[] rentAmounts = {780_000L, 780_000L, 780_000L};
		long[] telecomAmounts = {89_000L, 89_000L, 91_000L};
		long[] insuranceAmounts = {132_000L, 132_000L, 132_000L};
		long[] internalTransferAmounts = {250_000L, 300_000L, 350_000L};

		for (int index = 0; index < targetMonths.size(); index++) {
			YearMonth month = targetMonths.get(index);
			seeds.add(new AccountTransactionSeed(
				nextUniqueNo(month, sequence++),
				month.atDay(5).atTime(9, 0),
				1,
				"입금",
				"",
				salaryAmounts[index],
				"월급"
			));
			seeds.add(new AccountTransactionSeed(
				nextUniqueNo(month, sequence++),
				month.atDay(7).atTime(8, 40),
				2,
				"출금",
				"4400111122223333",
				rentAmounts[index],
				"월세"
			));
			seeds.add(new AccountTransactionSeed(
				nextUniqueNo(month, sequence++),
				month.atDay(12).atTime(10, 15),
				2,
				"출금",
				"5500222233334444",
				telecomAmounts[index],
				"휴대폰 요금"
			));
			seeds.add(new AccountTransactionSeed(
				nextUniqueNo(month, sequence++),
				month.atDay(18).atTime(14, 20),
				2,
				"출금",
				"6600333344445555",
				insuranceAmounts[index],
				"실손보험"
			));
			seeds.add(new AccountTransactionSeed(
				nextUniqueNo(month, sequence++),
				month.atDay(21).atTime(19, 10),
				2,
				"출금",
				helperAccountNo,
				internalTransferAmounts[index],
				"비상금 이체"
			));
		}

		YearMonth latestMonth = targetMonths.get(targetMonths.size() - 1);
		seeds.add(new AccountTransactionSeed(
			nextUniqueNo(latestMonth, sequence),
			latestMonth.atDay(25).atTime(16, 45),
			1,
			"입금",
			"",
			85_000L,
			"중고거래 정산"
		));
		return seeds.stream()
			.sorted(Comparator.comparing(AccountTransactionSeed::occurredAt))
			.toList();
	}

	private List<CardTransactionSeed> buildCardSeeds(List<YearMonth> targetMonths) {
		List<CardTransactionSeed> seeds = new ArrayList<>();
		long sequence = 2_000L;
		MerchantProfile netflix = new MerchantProfile(31_001L, "넷플릭스");
		MerchantProfile starbucks = new MerchantProfile(31_002L, "스타벅스 서울역점");
		MerchantProfile emart = new MerchantProfile(31_003L, "이마트 은평점");
		MerchantProfile kakaoTaxi = new MerchantProfile(31_004L, "카카오택시");
		MerchantProfile oliveYoung = new MerchantProfile(31_005L, "올리브영 강남점");
		MerchantProfile coupang = new MerchantProfile(31_006L, "쿠팡");
		MerchantProfile baemin = new MerchantProfile(31_007L, "배달의민족");
		MerchantProfile hospital = new MerchantProfile(31_008L, "서울아산병원");

		for (int index = 0; index < targetMonths.size(); index++) {
			YearMonth month = targetMonths.get(index);
			seeds.add(cardSeed(month, 1, 7, 10, 17_000L, sequence++, netflix));
			seeds.add(cardSeed(month, 4, 8, 20, 6_100L + index * 400L, sequence++, starbucks));
			seeds.add(cardSeed(month, 11, 7, 55, 5_800L + index * 300L, sequence++, starbucks));
			seeds.add(cardSeed(month, 8, 18, 40, 42_000L + index * 2_000L, sequence++, emart));
			seeds.add(cardSeed(month, 24, 19, 5, 38_500L + index * 1_500L, sequence++, emart));
			seeds.add(cardSeed(month, 9, 21, 15, 15_000L + index * 1_000L, sequence++, kakaoTaxi));
			seeds.add(cardSeed(month, 15, 13, 25, 18_900L + index * 700L, sequence++, oliveYoung));
			seeds.add(cardSeed(month, 22, 11, 10, 32_900L + index * 2_300L, sequence++, coupang));
			seeds.add(cardSeed(month, 27, 20, 30, 21_500L + index * 1_200L, sequence++, baemin));
		}

		YearMonth middleMonth = targetMonths.get(1);
		seeds.add(cardSeed(middleMonth, 17, 10, 5, 27_000L, sequence++, hospital));
		return seeds.stream()
			.sorted(Comparator.comparing(CardTransactionSeed::occurredAt))
			.toList();
	}

	private CardTransactionSeed cardSeed(
		YearMonth month,
		int dayOfMonth,
		int hour,
		int minute,
		long amount,
		long sequence,
		MerchantProfile merchantProfile
	) {
		return new CardTransactionSeed(
			nextUniqueNo(month, sequence),
			month.atDay(dayOfMonth).atTime(hour, minute),
			amount,
			merchantProfile
		);
	}

	private long nextUniqueNo(YearMonth month, long sequence) {
		return Long.parseLong(month.toString().replace("-", "") + String.format("%05d", sequence));
	}

	private List<AccountTransactionOverride> mapAccountTransactions(
		Account account,
		List<AccountTransactionSeed> seeds,
		Category defaultCategory
	) {
		List<AccountTransactionOverride> transactions = new ArrayList<>(seeds.size());
		long runningBalance = 0L;
		for (int index = 0; index < seeds.size(); index++) {
			AccountTransactionSeed seed = seeds.get(index);
			runningBalance += seed.transactionType() == 1 ? seed.amount() : -seed.amount();
			transactions.add(AccountTransactionOverride.builder()
				.id(Math.abs(Objects.hash("ADMIN_ACCOUNT", account.getId(), seed.transactionUniqueNo())))
				.category(defaultCategory)
				.transactionDateTime(seed.occurredAt())
				.transactionBalance(seed.amount())
				.transactionMemo(seed.counterpartyAccountNo())
				.transactionUniqueNo(seed.transactionUniqueNo())
				.transactionType(seed.transactionType())
				.transactionTypeName(seed.transactionTypeName())
				.transactionAfterBalance(runningBalance)
				.transactionSummary(seed.summary())
				.account(account)
				.build());
		}
		return transactions;
	}

	private List<CardTransactionOverride> mapCardTransactions(
		Card card,
		List<CardTransactionSeed> seeds,
		Category defaultCategory
	) {
		return java.util.stream.IntStream.range(0, seeds.size())
			.mapToObj(index -> {
				CardTransactionSeed seed = seeds.get(index);
				Merchant merchant = merchantRepository.findById(Math.toIntExact(seed.merchantProfile().merchantId()))
					.orElseGet(() -> merchantRepository.save(Merchant.builder()
						.id(Math.toIntExact(seed.merchantProfile().merchantId()))
						.name(seed.merchantProfile().merchantName())
						.build()));

				CardTransactionOverride transaction = CardTransactionOverride.builder()
					.id(Math.abs(Objects.hash("ADMIN_CARD", card.getCardUniqueNo(), seed.transactionUniqueNo())))
					.category(defaultCategory)
					.transactionDateTime(seed.occurredAt())
					.transactionBalance(seed.amount())
					.transactionMemo(seed.merchantProfile().merchantName())
					.transactionUniqueNo(seed.transactionUniqueNo())
					.merchant(merchant)
					.cardStatus(true)
					.billStatementsYn(true)
					.billStatementsStatus("BILLED")
					.card(card)
					.build();
				return transaction;
			})
			.toList();
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private Long toLocalCardUniqueNo(String cardUniqueNo) {
		long hashed = Math.abs((long)cardUniqueNo.hashCode());
		return hashed == 0 ? 1L : hashed;
	}

	private void validateRequest(AccountAdminDto.DataInitRequest request) {
		if (request == null
			|| !hasText(request.apiKey())
			|| !hasText(request.userApiKey())) {
			throw new AccountException(AccountErrorCode.INVALID_DATA_INIT_REQUEST);
		}
	}

	private record AccountTransactionSeed(
		Long transactionUniqueNo,
		LocalDateTime occurredAt,
		Integer transactionType,
		String transactionTypeName,
		String counterpartyAccountNo,
		Long amount,
		String summary
	) {
		private String categoryHintText() {
			return summary;
		}
	}

	private record CardTransactionSeed(
		Long transactionUniqueNo,
		LocalDateTime occurredAt,
		Long amount,
		MerchantProfile merchantProfile
	) {
	}

	private record MerchantProfile(
		Long merchantId,
		String merchantName
	) {
	}
}
