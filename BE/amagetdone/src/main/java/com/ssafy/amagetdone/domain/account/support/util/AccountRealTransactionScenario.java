package com.ssafy.amagetdone.domain.account.support.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class AccountRealTransactionScenario {

	private static final long OPENING_BALANCE = 420_000L;
	private static final DateTimeFormatter UNIQUE_NO_DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

	private static final List<AccountSeed> COMPLETED_MONTH_SEEDS = buildSeeds();

	public List<AccountSeed> completedMonthSeeds() {
		return COMPLETED_MONTH_SEEDS;
	}

	private static List<AccountSeed> buildSeeds() {
		List<SeedDraft> drafts = new ArrayList<>();
		drafts.addAll(decemberDrafts());
		drafts.addAll(januaryDrafts());
		drafts.addAll(februaryDrafts());
		drafts.addAll(marchDrafts());
		drafts.sort(Comparator.comparing(SeedDraft::transactionDateTime));

		long balance = OPENING_BALANCE;
		Map<String, Integer> daySequence = new HashMap<>();
		List<AccountSeed> chronological = new ArrayList<>();
		for (SeedDraft draft : drafts) {
			balance += draft.transactionType() == 1 ? draft.transactionBalance() : -draft.transactionBalance();
			String dayKey = draft.transactionDateTime().format(UNIQUE_NO_DAY_FORMAT);
			int sequence = daySequence.merge(dayKey, 1, Integer::sum);
			long uniqueNo = Long.parseLong(dayKey + String.format("%03d", sequence));
			chronological.add(new AccountSeed(
				draft.transactionDateTime(),
				uniqueNo,
				draft.transactionType(),
				draft.transactionMemo(),
				draft.transactionBalance(),
				balance,
				draft.transactionSummary()
			));
		}

		List<AccountSeed> descending = new ArrayList<>(chronological);
		descending.sort(Comparator.comparing(AccountSeed::transactionDateTime).reversed());
		return List.copyOf(descending);
	}

	private static List<SeedDraft> decemberDrafts() {
		return List.of(
			draft(2025, 12, 12, 9, 0, 1, "급여", 500_000, "삼성SSAFY"),
			draft(2025, 12, 12, 10, 10, 2, "주거관리비", 85_000, "관리비이체"),
			draft(2025, 12, 13, 12, 20, 2, "네이버페이충전 ((네이버파이낸셜(주)_펌뱅킹))", 30_000, "네이버페이충전"),
			draft(2025, 12, 14, 18, 15, 2, "삼성월렛머니 (((주)우리은행_펌뱅킹))", 20_000, "삼성월렛머니"),
			draft(2025, 12, 15, 8, 30, 2, "KT통신요금02", 15_140, "KT통신요금"),
			draft(2025, 12, 16, 13, 10, 1, "생활지원금", 80_000, "고용노동부"),
			draft(2025, 12, 17, 19, 25, 2, "토스페이 ((토스_펌뱅킹))", 18_000, "토스페이"),
			draft(2025, 12, 18, 11, 40, 2, "하나카드결제", 96_439, "하나카드결제"),
			draft(2025, 12, 19, 20, 5, 1, "친구정산", 42_000, "정00"),
			draft(2025, 12, 20, 14, 20, 2, "카카오페이 ((주식회사 카카오페이_펌뱅킹))", 12_000, "카카오페이"),
			draft(2025, 12, 21, 9, 15, 2, "적금자동이체", 50_000, "자유적금이체"),
			draft(2025, 12, 22, 16, 50, 1, "청년월세지원", 100_000, "구미형 청년월세(2"),
			draft(2025, 12, 23, 12, 5, 2, "네이버페이충전 ((네이버파이낸셜(주)_펌뱅킹))", 25_000, "네이버페이충전"),
			draft(2025, 12, 24, 18, 30, 2, "토스페이 ((비바리퍼블리카(토스)-오픈뱅킹))", 15_000, "토스페이"),
			draft(2025, 12, 25, 8, 45, 1, "현금IC캐시백", 3_000, "신한할인캐쉬백"),
			draft(2025, 12, 26, 19, 10, 2, "삼성월렛머니 (((주)우리은행_펌뱅킹))", 35_000, "삼성월렛머니"),
			draft(2025, 12, 27, 13, 35, 1, "개인송금", 60_000, "김00"),
			draft(2025, 12, 28, 17, 0, 2, "하나카드결제", 84_310, "하나카드결제")
		);
	}

	private static List<SeedDraft> januaryDrafts() {
		return List.of(
			draft(2026, 1, 3, 9, 5, 1, "급여", 500_000, "삼성SSAFY"),
			draft(2026, 1, 4, 10, 25, 2, "주거관리비", 85_000, "관리비이체"),
			draft(2026, 1, 5, 12, 0, 2, "네이버페이충전 ((네이버파이낸셜(주)_펌뱅킹))", 40_000, "네이버페이충전"),
			draft(2026, 1, 6, 18, 5, 2, "삼성월렛머니 (((주)우리은행_펌뱅킹))", 25_000, "삼성월렛머니"),
			draft(2026, 1, 7, 8, 20, 2, "KT통신요금02", 15_140, "KT통신요금"),
			draft(2026, 1, 8, 11, 15, 1, "생활지원금", 100_000, "고용구미고용"),
			draft(2026, 1, 9, 20, 10, 2, "토스페이 ((토스_펌뱅킹))", 22_000, "토스페이"),
			draft(2026, 1, 10, 13, 35, 2, "하나카드결제", 101_552, "하나카드결제"),
			draft(2026, 1, 11, 18, 40, 1, "친구정산", 38_000, "서00"),
			draft(2026, 1, 12, 9, 50, 2, "카카오페이 ((주식회사 카카오페이_펌뱅킹))", 15_000, "카카오페이"),
			draft(2026, 1, 13, 15, 0, 2, "적금자동이체", 50_000, "자유적금이체"),
			draft(2026, 1, 14, 10, 10, 1, "청년월세지원", 100_000, "구미형 청년월세(2"),
			draft(2026, 1, 15, 17, 20, 2, "네이버페이충전 ((네이버파이낸셜(주)_펌뱅킹))", 30_000, "네이버페이충전"),
			draft(2026, 1, 16, 21, 10, 2, "토스페이 ((비바리퍼블리카(토스)-오픈뱅킹))", 17_000, "토스페이"),
			draft(2026, 1, 17, 8, 55, 1, "환급금", 12_000, "지역농축협(8801)"),
			draft(2026, 1, 18, 19, 30, 2, "삼성월렛머니 (((주)우리은행_펌뱅킹))", 45_000, "삼성월렛머니"),
			draft(2026, 1, 19, 13, 15, 1, "개인송금", 70_000, "정00"),
			draft(2026, 1, 20, 18, 25, 2, "하나카드결제", 92_340, "하나카드결제")
		);
	}

	private static List<SeedDraft> februaryDrafts() {
		return List.of(
			draft(2026, 2, 1, 9, 0, 1, "급여", 520_000, "삼성SSAFY"),
			draft(2026, 2, 2, 10, 10, 2, "주거관리비", 85_000, "관리비이체"),
			draft(2026, 2, 3, 12, 20, 2, "네이버페이충전 ((네이버파이낸셜(주)_펌뱅킹))", 35_000, "네이버페이충전"),
			draft(2026, 2, 4, 18, 15, 2, "삼성월렛머니 (((주)우리은행_펌뱅킹))", 30_000, "삼성월렛머니"),
			draft(2026, 2, 5, 8, 30, 2, "KT통신요금02", 15_140, "KT통신요금"),
			draft(2026, 2, 6, 13, 10, 1, "생활지원금", 120_000, "고용노동부"),
			draft(2026, 2, 7, 19, 25, 2, "토스페이 ((토스_펌뱅킹))", 19_000, "토스페이"),
			draft(2026, 2, 8, 11, 40, 2, "하나카드결제", 110_240, "하나카드결제"),
			draft(2026, 2, 9, 20, 5, 1, "친구정산", 35_000, "오00"),
			draft(2026, 2, 10, 14, 20, 2, "카카오페이 ((주식회사 카카오페이_펌뱅킹))", 18_000, "카카오페이"),
			draft(2026, 2, 11, 9, 15, 2, "적금자동이체", 60_000, "자유적금이체"),
			draft(2026, 2, 12, 16, 50, 1, "청년월세지원", 100_000, "구미형 청년월세(2"),
			draft(2026, 2, 13, 12, 5, 2, "네이버페이충전 ((네이버파이낸셜(주)_펌뱅킹))", 28_000, "네이버페이충전"),
			draft(2026, 2, 14, 18, 30, 2, "토스페이 ((비바리퍼블리카(토스)-오픈뱅킹))", 16_000, "토스페이"),
			draft(2026, 2, 15, 8, 45, 1, "현금IC캐시백", 4_000, "신한할인캐쉬백"),
			draft(2026, 2, 16, 19, 10, 2, "삼성월렛머니 (((주)우리은행_펌뱅킹))", 50_000, "삼성월렛머니"),
			draft(2026, 2, 17, 13, 35, 1, "개인송금", 65_000, "하00"),
			draft(2026, 2, 18, 17, 0, 2, "하나카드결제", 98_610, "하나카드결제")
		);
	}

	private static List<SeedDraft> marchDrafts() {
		return List.of(
			draft(2026, 3, 3, 9, 5, 1, "급여", 520_000, "삼성SSAFY"),
			draft(2026, 3, 4, 10, 25, 2, "주거관리비", 85_000, "관리비이체"),
			draft(2026, 3, 5, 12, 0, 2, "네이버페이충전 ((네이버파이낸셜(주)_펌뱅킹))", 30_000, "네이버페이충전"),
			draft(2026, 3, 6, 18, 5, 2, "삼성월렛머니 (((주)우리은행_펌뱅킹))", 25_000, "삼성월렛머니"),
			draft(2026, 3, 7, 8, 20, 2, "KT통신요금02", 15_140, "KT통신요금"),
			draft(2026, 3, 8, 11, 15, 1, "생활지원금", 100_000, "고용구미고용"),
			draft(2026, 3, 9, 20, 10, 2, "토스페이 ((토스_펌뱅킹))", 22_000, "토스페이"),
			draft(2026, 3, 10, 13, 35, 2, "하나카드결제", 112_450, "하나카드결제"),
			draft(2026, 3, 11, 18, 40, 1, "친구정산", 38_000, "김00"),
			draft(2026, 3, 12, 9, 50, 2, "카카오페이 ((주식회사 카카오페이_펌뱅킹))", 16_000, "카카오페이"),
			draft(2026, 3, 13, 15, 0, 2, "적금자동이체", 60_000, "자유적금이체"),
			draft(2026, 3, 14, 10, 10, 1, "청년월세지원", 100_000, "구미형 청년월세(2"),
			draft(2026, 3, 15, 17, 20, 2, "네이버페이충전 ((네이버파이낸셜(주)_펌뱅킹))", 26_000, "네이버페이충전"),
			draft(2026, 3, 17, 21, 10, 2, "토스페이 ((비바리퍼블리카(토스)-오픈뱅킹))", 17_000, "토스페이"),
			draft(2026, 3, 18, 8, 55, 1, "환급금", 12_000, "지역농축협(8801)"),
			draft(2026, 3, 20, 18, 25, 2, "하나카드결제", 97_540, "하나카드결제")
		);
	}

	private static SeedDraft draft(
		int year,
		int month,
		int day,
		int hour,
		int minute,
		int transactionType,
		String transactionMemo,
		long transactionBalance,
		String transactionSummary
	) {
		return new SeedDraft(
			LocalDateTime.of(year, month, day, hour, minute),
			transactionType,
			transactionMemo,
			transactionBalance,
			transactionSummary
		);
	}

	private record SeedDraft(
		LocalDateTime transactionDateTime,
		int transactionType,
		String transactionMemo,
		long transactionBalance,
		String transactionSummary
	) {
	}

	public record AccountSeed(
		LocalDateTime transactionDateTime,
		Long transactionUniqueNo,
		Integer transactionType,
		String transactionMemo,
		Long transactionBalance,
		Long transactionAfterBalance,
		String transactionSummary
	) {
	}
}
