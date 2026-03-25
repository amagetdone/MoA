package com.ssafy.amagetdone.domain.account.application.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ssafy.amagetdone.domain.account.api.dto.AccountAdminDto;
import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.AccountSsafyApiProperties;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyAdminCreditCardApiClient;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.entity.Merchant;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.model.repository.MerchantRepository;
import com.ssafy.amagetdone.domain.transaction.application.service.TransactionCategoryAssignmentService;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.CardTransactionDto;
import com.ssafy.amagetdone.domain.transaction.model.entity.CardTransactionOverride;
import com.ssafy.amagetdone.domain.transaction.model.repository.CardTransactionOverrideRepository;
import com.ssafy.amagetdone.domain.transaction.support.util.TransactionMerchantTextResolver;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CancelAdminCardPaymentUseCase {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

	private final CardRepository cardRepository;
	private final CardTransactionOverrideRepository cardTransactionOverrideRepository;
	private final MerchantRepository merchantRepository;
	private final SsafyAdminCreditCardApiClient ssafyAdminCreditCardApiClient;
	private final TransactionCategoryAssignmentService transactionCategoryAssignmentService;
	private final AccountSsafyApiProperties properties;

	public CancelAdminCardPaymentUseCase(
		CardRepository cardRepository,
		CardTransactionOverrideRepository cardTransactionOverrideRepository,
		MerchantRepository merchantRepository,
		SsafyAdminCreditCardApiClient ssafyAdminCreditCardApiClient,
		TransactionCategoryAssignmentService transactionCategoryAssignmentService,
		AccountSsafyApiProperties properties
	) {
		this.cardRepository = cardRepository;
		this.cardTransactionOverrideRepository = cardTransactionOverrideRepository;
		this.merchantRepository = merchantRepository;
		this.ssafyAdminCreditCardApiClient = ssafyAdminCreditCardApiClient;
		this.transactionCategoryAssignmentService = transactionCategoryAssignmentService;
		this.properties = properties;
	}

	@Transactional
	public AccountAdminDto.PaymentResponse execute(Integer userId, AccountAdminDto.CardPaymentCancelRequest request) {
		Card card = cardRepository.findByCardNo(request.cardNo())
			.filter(foundCard -> foundCard.getWithdrawalAccount().getUser().getId() == userId)
			.orElseThrow(() -> new AccountException(AccountErrorCode.CARD_NOT_FOUND));

		CardTransactionDto.Item item = ssafyAdminCreditCardApiClient.cancelCardTransaction(
			properties.getApiKey(),
			card.getWithdrawalAccount().getUser().getUserKey(),
			card.getCardNo(),
			card.getCvc(),
			request.transactionUniqueNo()
		);

		CardTransactionOverride override = CardTransactionOverride.builder()
			.id(buildTransactionId(card.getCardUniqueNo(), item))
			.category(resolveCategory(item))
			.transactionDateTime(toDateTime(item.transactionDate(), item.transactionTime()))
			.transactionBalance(item.transactionBalance())
			.transactionMemo(item.merchantName())
			.transactionUniqueNo(item.transactionUniqueNo())
			.merchant(resolveMerchant(item.merchantId(), item.merchantName()))
			.cardStatus(toBoolean(item.cardStatus()))
			.billStatementsYn(toBoolean(item.billStatementsYn()))
			.billStatementsStatus(item.billStatementsStatus())
			.card(card)
			.build();
		cardTransactionOverrideRepository.save(override);

		return new AccountAdminDto.PaymentResponse(
			"card",
			card.getCardNo(),
			item.transactionUniqueNo(),
			item.transactionDate(),
			item.transactionTime(),
			item.transactionBalance(),
			item.billStatementsStatus()
		);
	}

	private Category resolveCategory(CardTransactionDto.Item item) {
		String sourceText = TransactionMerchantTextResolver.resolve(item);
		Map<String, Category> categories = transactionCategoryAssignmentService.assignCategories(List.of(sourceText));
		Category category = categories.get(sourceText);
		if (category == null) {
			throw new AccountException(AccountErrorCode.ACCOUNT_ERROR);
		}
		return category;
	}

	private Merchant resolveMerchant(Long merchantId, String merchantName) {
		int resolvedMerchantId = Math.toIntExact(merchantId);
		return merchantRepository.findById(resolvedMerchantId)
			.orElseGet(() -> merchantRepository.save(Merchant.builder()
				.id(resolvedMerchantId)
				.name(merchantName)
				.build()));
	}

	private LocalDateTime toDateTime(String transactionDate, String transactionTime) {
		return LocalDateTime.of(
			LocalDate.parse(transactionDate, DATE_FORMATTER),
			LocalTime.parse(normalizeTime(transactionTime), TIME_FORMATTER)
		);
	}

	private String normalizeTime(String transactionTime) {
		if (transactionTime == null || transactionTime.isBlank()) {
			return "000000";
		}
		return transactionTime.length() == 6 ? transactionTime : String.format("%1$" + 6 + "s", transactionTime).replace(' ', '0');
	}

	private boolean toBoolean(String value) {
		if (value == null) {
			return false;
		}
		return "Y".equalsIgnoreCase(value)
			|| "TRUE".equalsIgnoreCase(value)
			|| "1".equals(value);
	}

	private Integer buildTransactionId(Long cardUniqueNo, CardTransactionDto.Item item) {
		return Math.floorMod(
			Objects.hash("ADMIN_CARD_PAYMENT_CANCEL", cardUniqueNo, item.transactionUniqueNo(), item.transactionDate(), item.transactionTime()),
			Integer.MAX_VALUE - 1
		) + 1;
	}
}
