package com.ssafy.amagetdone.domain.accountverification.application.listener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.ssafy.amagetdone.domain.accountverification.application.event.VerificationRequestedEvent;
import com.ssafy.amagetdone.domain.notification.application.service.PushNotificationService;
import com.ssafy.amagetdone.domain.transaction.infra.client.SsafyDemandDepositTransactionApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.DemandDepositTransactionDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class VerificationRequestedEventListener {

	private static final int MAX_POLL_ATTEMPTS = 10;
	private static final long POLL_DELAY_MILLIS = 500L;
	private static final long MATCH_WINDOW_SECONDS = 10L;
	private static final String TRANSACTION_TYPE_ALL = "A";
	private static final String DESCENDING_ORDER = "DESC";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

	private final SsafyDemandDepositTransactionApiClient demandDepositTransactionApiClient;
	private final PushNotificationService pushNotificationService;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleVerificationRequested(VerificationRequestedEvent event) {
		for (int attempt = 1; attempt <= MAX_POLL_ATTEMPTS; attempt++) {
			Optional<String> verificationText = pollVerificationText(event);
			if (verificationText.isPresent()) {
				PushNotificationService.SendResult result =
					pushNotificationService.sendVerificationAlarmDemoNotification(verificationText.get());
				log.info(
					"Verification request demo push result. userId={}, accountNo={}, attempt={}, result={}",
					event.userId(),
					event.accountNo(),
					attempt,
					result
				);
				return;
			}

			sleepBeforeNextAttempt(event, attempt);
		}

		log.warn(
			"Verification request demo push skipped because verification deposit was not detected in time. userId={}, accountNo={}",
			event.userId(),
			event.accountNo()
		);
	}

	private Optional<String> pollVerificationText(VerificationRequestedEvent event) {
		LocalDate startDate = event.requestedAt().toLocalDate();
		LocalDate endDate = LocalDate.now();
		DemandDepositTransactionDto.Response response = demandDepositTransactionApiClient.inquireTransactions(
			new DemandDepositTransactionDto.Request(
				event.userKey(),
				event.accountNo(),
				startDate.format(DATE_FORMATTER),
				endDate.format(DATE_FORMATTER),
				TRANSACTION_TYPE_ALL,
				DESCENDING_ORDER
			)
		);

		return response.transactions().stream()
			.filter(item -> isVerificationDeposit(item, event))
			.map(this::resolveVerificationText)
			.flatMap(Optional::stream)
			.findFirst();
	}

	private boolean isVerificationDeposit(
		DemandDepositTransactionDto.Item item,
		VerificationRequestedEvent event
	) {
		if (item == null || item.transactionBalance() == null || item.transactionBalance() != 1L) {
			return false;
		}
		if (!"입금".equals(item.transactionTypeName())) {
			return false;
		}
		LocalDateTime transactionDateTime = toDateTime(item.transactionDate(), item.transactionTime());
		if (transactionDateTime.isBefore(event.requestedAt().minusSeconds(MATCH_WINDOW_SECONDS))) {
			return false;
		}
		return resolveVerificationText(item)
			.map(value -> value.toUpperCase().startsWith(event.authText().toUpperCase()))
			.orElse(false);
	}

	private Optional<String> resolveVerificationText(DemandDepositTransactionDto.Item item) {
		String summary = item.transactionSummary();
		if (summary != null && !summary.isBlank() && summary.toUpperCase().startsWith("AMAGETDONE")) {
			return Optional.of(summary.trim());
		}
		String memo = item.transactionMemo();
		if (memo != null && !memo.isBlank() && memo.toUpperCase().startsWith("AMAGETDONE")) {
			return Optional.of(memo.trim());
		}
		return Optional.empty();
	}

	private LocalDateTime toDateTime(String transactionDate, String transactionTime) {
		return LocalDateTime.of(
			LocalDate.parse(transactionDate, DATE_FORMATTER),
			java.time.LocalTime.parse(normalizeTime(transactionTime), TIME_FORMATTER)
		);
	}

	private String normalizeTime(String transactionTime) {
		if (transactionTime == null) {
			return "000000";
		}
		return transactionTime.length() == 6 ? transactionTime : String.format("%1$" + 6 + "s", transactionTime).replace(' ', '0');
	}

	private void sleepBeforeNextAttempt(VerificationRequestedEvent event, int attempt) {
		if (attempt >= MAX_POLL_ATTEMPTS) {
			return;
		}
		try {
			Thread.sleep(POLL_DELAY_MILLIS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			log.warn(
				"Verification request demo polling interrupted. userId={}, accountNo={}, attempt={}",
				event.userId(),
				event.accountNo(),
				attempt,
				exception
			);
		}
	}
}
