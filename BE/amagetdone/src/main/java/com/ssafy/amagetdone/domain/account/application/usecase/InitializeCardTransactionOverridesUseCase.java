package com.ssafy.amagetdone.domain.account.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.model.repository.MerchantRepository;
import com.ssafy.amagetdone.domain.account.support.util.CardRealTransactionScenario;
import com.ssafy.amagetdone.domain.account.support.util.CardRealTransactionScenario.CardSeed;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionCategoryAssignmentService;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.model.repository.TransactionAnalysisResultRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitializeCardTransactionOverridesUseCase {

	private static final String CANCELED_STATUS = "\uCDE8\uC18C";
	private static final String PURCHASED_STATUS = "\uB9E4\uC785";

	private final CardRepository cardRepository;
	private final MerchantRepository merchantRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final TransactionAnalysisResultRepository transactionAnalysisResultRepository;
	private final CardRealTransactionScenario cardRealTransactionScenario;
	private final TransactionCategoryAssignmentService transactionCategoryAssignmentService;
	private final CardTransactionOverrideBulkSavePort cardTransactionOverrideBulkSavePort;

	public InitializeCardTransactionOverridesUseCase(
		CardRepository cardRepository,
		MerchantRepository merchantRepository,
		CardTransactionOverrideRepository cardTransactionOverrideRepository,
		TransactionAnalysisResultRepository transactionAnalysisResultRepository,
		CardRealTransactionScenario cardRealTransactionScenario,
		TransactionCategoryAssignmentService transactionCategoryAssignmentService,
		CardTransactionOverrideBulkSavePort cardTransactionOverrideBulkSavePort
	) {
		this.cardRepository = cardRepository;
		this.merchantRepository = merchantRepository;
		this.cardTransactionOverrideRepository = cardTransactionOverrideRepository;
		this.transactionAnalysisResultRepository = transactionAnalysisResultRepository;
		this.cardRealTransactionScenario = cardRealTransactionScenario;
		this.transactionCategoryAssignmentService = transactionCategoryAssignmentService;
		this.cardTransactionOverrideBulkSavePort = cardTransactionOverrideBulkSavePort;
	}

	@Transactional
	public AccountAdminDto.TransactionInitResponse execute(AccountAdminDto.CardTransactionInitRequest request) {
		validateRequest(request == null ? null : request.cardNo());
		Card card = cardRepository.findByCardNo(request.cardNo())
			.orElseThrow(() -> new AccountException(AccountErrorCode.CARD_NOT_FOUND));
		List<CardSeed> seeds = cardRealTransactionScenario.completedMonthSeeds();
		if (seeds.isEmpty()) {
			throw new AccountException(AccountErrorCode.TRANSACTION_INIT_SEED_NOT_FOUND);
		}
		Map<Integer, Merchant> seededMerchantsById = loadSeededMerchants(seeds);
		Map<String, Category> assignedCategories = transactionCategoryAssignmentService.assignCategories(
			seeds.stream()
				.map(CardSeed::merchantName)
				.toList()
		);

		deleteExistingAnalysisResults(card.getCardUniqueNo());
		cardTransactionOverrideRepository.deleteByCard_CardUniqueNo(card.getCardUniqueNo());
		List<CardTransactionOverride> transactions = seeds.stream()
			.map(seed -> CardTransactionOverride.builder()
				.id(toEntityId("CARD_INIT", card.getCardUniqueNo(), seed.transactionUniqueNo()))
				.category(resolveCategory(assignedCategories, seed.merchantName()))
				.transactionDateTime(seed.transactionDateTime())
				.transactionBalance(seed.transactionBalance())
				.transactionMemo(seed.merchantName())
				.transactionUniqueNo(seed.transactionUniqueNo())
				.merchant(resolveMerchant(seed, seededMerchantsById))
				.cardStatus(!CANCELED_STATUS.equals(seed.transactionStatus()))
				.billStatementsYn(PURCHASED_STATUS.equals(seed.purchaseStatus()))
				.billStatementsStatus(seed.transactionStatus())
				.card(card)
				.build())
			.map(CardTransactionOverride.class::cast)
			.toList();
		cardTransactionOverrideBulkSavePort.saveAll(transactions);

		return new AccountAdminDto.TransactionInitResponse(
			"card",
			card.getCardNo(),
			seeds.size()
		);
	}

	private Map<Integer, Merchant> loadSeededMerchants(List<CardSeed> seeds) {
		Map<Integer, Merchant> merchantsById = merchantRepository.findAllById(
			seeds.stream()
				.map(CardSeed::merchantId)
				.distinct()
				.toList()
		).stream().collect(Collectors.toMap(Merchant::getId, Function.identity()));
		boolean hasMissingMerchant = seeds.stream()
			.map(CardSeed::merchantId)
			.anyMatch(merchantId -> !merchantsById.containsKey(merchantId));
		if (hasMissingMerchant) {
			throw new AccountException(AccountErrorCode.ACCOUNT_ERROR);
		}
		return merchantsById;
	}

	private Merchant resolveMerchant(CardSeed seed, Map<Integer, Merchant> merchantsById) {
		Merchant merchant = merchantsById.get(seed.merchantId());
		if (merchant == null) {
			throw new AccountException(AccountErrorCode.ACCOUNT_ERROR);
		}
		return merchant;
	}

	private void validateRequest(String cardNo) {
		if (cardNo == null || cardNo.isBlank()) {
			throw new AccountException(AccountErrorCode.INVALID_DATA_INIT_REQUEST);
		}
	}

	private void deleteExistingAnalysisResults(Long cardUniqueNo) {
		List<Integer> existingTransactionIds = cardTransactionOverrideRepository.findAllByCard_CardUniqueNo(cardUniqueNo).stream()
			.map(CardTransactionOverride::getId)
			.toList();
		if (!existingTransactionIds.isEmpty()) {
			transactionAnalysisResultRepository.deleteByTransaction_IdIn(existingTransactionIds);
		}
	}

	private int toEntityId(String prefix, Long targetId, Long transactionUniqueNo) {
		return Math.floorMod(Objects.hash(prefix, targetId, transactionUniqueNo), Integer.MAX_VALUE - 1) + 1;
	}

	private Category resolveCategory(Map<String, Category> assignedCategories, String sourceText) {
		Category category = assignedCategories.get(sourceText);
		if (category == null) {
			throw new AccountException(AccountErrorCode.ACCOUNT_ERROR);
		}
		return category;
	}
}
