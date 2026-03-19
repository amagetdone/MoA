package com.ssafy.amagetdone.domain.account.application.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.ssafy.amagetdone.domain.account.exception.AccountErrorCode;
import com.ssafy.amagetdone.domain.account.exception.AccountException;
import com.ssafy.amagetdone.domain.account.infra.client.SsafyCreditCardQueryApiClient;
import com.ssafy.amagetdone.domain.account.infra.client.dto.CreditCardQueryDto;
import com.ssafy.amagetdone.domain.account.model.entity.Account;
import com.ssafy.amagetdone.domain.account.model.entity.Card;
import com.ssafy.amagetdone.domain.account.model.repository.AccountRepository;
import com.ssafy.amagetdone.domain.account.model.repository.CardRepository;
import com.ssafy.amagetdone.domain.account.support.util.LocalCardUniqueNoMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SyncLinkedCardsUseCase {

	private static final Logger log = LoggerFactory.getLogger(SyncLinkedCardsUseCase.class);
	private static final DateTimeFormatter CARD_EXPIRY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

	private final AccountRepository accountRepository;
	private final CardRepository cardRepository;
	private final SsafyCreditCardQueryApiClient ssafyCreditCardQueryApiClient;
	private final LocalCardUniqueNoMapper localCardUniqueNoMapper;

	public SyncLinkedCardsUseCase(
		AccountRepository accountRepository,
		CardRepository cardRepository,
		SsafyCreditCardQueryApiClient ssafyCreditCardQueryApiClient,
		LocalCardUniqueNoMapper localCardUniqueNoMapper
	) {
		this.accountRepository = accountRepository;
		this.cardRepository = cardRepository;
		this.ssafyCreditCardQueryApiClient = ssafyCreditCardQueryApiClient;
		this.localCardUniqueNoMapper = localCardUniqueNoMapper;
	}

	public void execute(Integer userId, String userKey, String linkedAccountNo) {
		Account linkedAccount = accountRepository.findActiveLinkedAccountByUserIdAndAccountNo(userId, linkedAccountNo)
			.orElseThrow(() -> new AccountException(AccountErrorCode.ACCOUNT_NOT_FOUND));

		CreditCardQueryDto.Response response = ssafyCreditCardQueryApiClient.inquireLinkedCards(
			new CreditCardQueryDto.Request(userKey)
		);
		List<CreditCardQueryDto.CardItem> remoteCards = response.cards();
		if (remoteCards.isEmpty()) {
			return;
		}

		List<CreditCardQueryDto.CardItem> linkedCards = remoteCards.stream()
			.filter(card -> linkedAccountNo.equals(card.withdrawalAccountNo()))
			.toList();
		if (linkedCards.isEmpty()) {
			log.error(
				"Linked card sync failed: no cards matched linked account. userId={}, accountNo={}, remoteTotalCardCount={}, filteredCardCount=0, failureReason={}",
				userId,
				linkedAccountNo,
				remoteCards.size(),
				AccountErrorCode.LINKED_ACCOUNT_CARD_NOT_FOUND.code()
			);
			throw new AccountException(AccountErrorCode.LINKED_ACCOUNT_CARD_NOT_FOUND);
		}

		try {
			for (CreditCardQueryDto.CardItem linkedCard : linkedCards) {
				upsertCard(linkedAccount, linkedCard);
			}
		} catch (AccountException exception) {
			log.error(
				"Linked card sync failed during upsert. userId={}, accountNo={}, remoteTotalCardCount={}, filteredCardCount={}, failureReason={}",
				userId,
				linkedAccountNo,
				remoteCards.size(),
				linkedCards.size(),
				exception.getErrorCode().code(),
				exception
			);
			throw new AccountException(AccountErrorCode.LINKED_CARD_SYNC_FAILED, exception);
		} catch (RuntimeException exception) {
			log.error(
				"Linked card sync failed during upsert. userId={}, accountNo={}, remoteTotalCardCount={}, filteredCardCount={}, failureReason=unexpected_runtime_exception",
				userId,
				linkedAccountNo,
				remoteCards.size(),
				linkedCards.size(),
				exception
			);
			throw new AccountException(AccountErrorCode.LINKED_CARD_SYNC_FAILED, exception);
		}
	}

	private void upsertCard(Account linkedAccount, CreditCardQueryDto.CardItem remoteCard) {
		Long localCardUniqueNo = localCardUniqueNoMapper.toLocalId(remoteCard.cardUniqueNo());
		LocalDateTime cardExpiryDate = parseCardExpiryDate(remoteCard.cardExpiryDate());
		String cardDescription = remoteCard.cardDescription() == null ? "" : remoteCard.cardDescription();

		Card card = cardRepository.findById(localCardUniqueNo)
			.map(existingCard -> {
				existingCard.sync(
					linkedAccount,
					remoteCard.cvc(),
					remoteCard.cardNo(),
					remoteCard.cardName(),
					remoteCard.cardIssuerCode(),
					remoteCard.cardIssuerName(),
					cardExpiryDate,
					cardDescription,
					remoteCard.withdrawalDate(),
					remoteCard.baselinePerformance(),
					remoteCard.maxBenefitLimit()
				);
				return existingCard;
			})
			.orElseGet(() -> Card.create(
				localCardUniqueNo,
				linkedAccount,
				remoteCard.cvc(),
				remoteCard.cardNo(),
				remoteCard.cardName(),
				remoteCard.cardIssuerCode(),
				remoteCard.cardIssuerName(),
				cardExpiryDate,
				cardDescription,
				remoteCard.withdrawalDate(),
				remoteCard.baselinePerformance(),
				remoteCard.maxBenefitLimit()
			));
		cardRepository.save(card);
	}

	private LocalDateTime parseCardExpiryDate(String cardExpiryDate) {
		try {
			return LocalDate.parse(cardExpiryDate, CARD_EXPIRY_FORMATTER).atStartOfDay();
		} catch (DateTimeParseException | NullPointerException exception) {
			throw new AccountException(AccountErrorCode.LINKED_CARD_SYNC_FAILED, exception);
		}
	}
}
