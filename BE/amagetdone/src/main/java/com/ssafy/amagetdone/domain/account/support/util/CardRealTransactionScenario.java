package com.ssafy.amagetdone.domain.account.support.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class CardRealTransactionScenario {

	private static final DateTimeFormatter UNIQUE_NO_DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final String PURCHASED_STATUS = "매입";
	private static final String PENDING_STATUS = "미매입";
	private static final String NORMAL_STATUS = "정상";
	private static final String CANCELED_STATUS = "취소";

	private static final List<MerchantDraft> TRANSPORT_MERCHANTS = List.of(
		merchant(202001, "대구교통카드", 1_450),
		merchant(202002, "경북시내버스", 1_700),
		merchant(202003, "카카오T 택시", 8_900),
		merchant(202004, "코레일톡", 12_800),
		merchant(202005, "SRT", 18_900),
		merchant(202006, "타슈 자전거", 1_500),
		merchant(202007, "티머니 충전", 5_000),
		merchant(202008, "도심 공영주차장", 3_200),
		merchant(202009, "쏘카", 12_400),
		merchant(202010, "UT 택시", 9_300)
	);

	private static final List<MerchantDraft> FOOD_MERCHANTS = List.of(
		merchant(101001, "맘스터치 경북대점", 8_900),
		merchant(101002, "한솥도시락 대현점", 7_500),
		merchant(101003, "김밥천국 산격점", 6_800),
		merchant(101004, "롯데리아 복현점", 9_400),
		merchant(101005, "서브웨이 칠성점", 9_900),
		merchant(101006, "이삭토스트 대구캠퍼스", 4_600),
		merchant(101007, "샐러디 동성로점", 9_800),
		merchant(101008, "버거킹 엑스코점", 10_500),
		merchant(101009, "역전우동 칠곡점", 7_200),
		merchant(101010, "홍콩반점 대현점", 8_500),
		merchant(101011, "엽기떡볶이 복현점", 15_000),
		merchant(101012, "배달의민족 결제", 17_900),
		merchant(101013, "쿠팡이츠 주문", 18_300),
		merchant(101014, "본죽 경북대병원점", 11_000),
		merchant(101015, "교동짬뽕 산격점", 9_500),
		merchant(101016, "빽다방 샌드위치", 6_400),
		merchant(101017, "죠스떡볶이 대구대점", 7_700),
		merchant(101018, "명륜진사갈비 복현점", 18_900),
		merchant(101019, "두찜 대구북구점", 22_500),
		merchant(101020, "청년다방 대학로점", 14_800)
	);

	private static final List<MerchantDraft> CONVENIENCE_MERCHANTS = List.of(
		merchant(102001, "CU 경북대북문점", 2_100),
		merchant(102002, "GS25 복현중앙점", 3_400),
		merchant(102003, "세븐일레븐 산격점", 2_800),
		merchant(102004, "이마트24 대현점", 3_100),
		merchant(102005, "CU 대구캠퍼스점", 2_500),
		merchant(102006, "GS25 야간점", 4_200),
		merchant(102007, "미니스톱 산격로점", 2_900),
		merchant(102008, "노브랜드 익스프레스", 8_900),
		merchant(102009, "편의점 택배결제", 4_700),
		merchant(102010, "CU 도시락결제", 5_500)
	);

	private static final List<MerchantDraft> CAFE_MERCHANTS = List.of(
		merchant(103001, "스타벅스 경북대점", 5_200),
		merchant(103002, "투썸플레이스 복현점", 6_100),
		merchant(103003, "메가커피 대현점", 3_400),
		merchant(103004, "컴포즈커피 산격점", 3_100),
		merchant(103005, "빽다방 복현로점", 2_900),
		merchant(103006, "할리스 칠성점", 5_800),
		merchant(103007, "이디야 경북대북문점", 4_100),
		merchant(103008, "설빙 동성로점", 12_800),
		merchant(103009, "요거트월드 대현점", 6_900),
		merchant(103010, "공차 칠곡점", 5_600)
	);

	private static final List<MerchantDraft> ONLINE_MERCHANTS = List.of(
		merchant(104001, "네이버페이 주문", 12_900),
		merchant(104002, "쿠팡 정기결제", 9_800),
		merchant(104003, "마켓컬리", 21_500),
		merchant(104004, "넷플릭스", 17_000),
		merchant(104005, "유튜브프리미엄", 14_900),
		merchant(104006, "멜론 이용권", 10_900),
		merchant(104007, "배달의민족 배민클럽", 3_990),
		merchant(104008, "오늘의집 스토어", 24_300),
		merchant(104009, "무신사 스토어", 36_000),
		merchant(104010, "교보문고 ebook", 8_500)
	);

	private static final List<MerchantDraft> SHOPPING_MERCHANTS = List.of(
		merchant(105001, "올리브영 동성로점", 18_900),
		merchant(105002, "다이소 대구대현점", 7_000),
		merchant(105003, "이마트 만촌점", 28_500),
		merchant(105004, "롯데마트 칠성점", 31_800),
		merchant(105005, "무인양품 동대구점", 22_900),
		merchant(105006, "유니클로 대구점", 39_900),
		merchant(105007, "탑텐 경북대점", 27_000),
		merchant(105008, "아트박스 복합몰", 11_500),
		merchant(105009, "교보핫트랙스", 16_400),
		merchant(105010, "노브랜드 대구점", 19_800)
	);

	private static final List<MerchantDraft> LIFE_MERCHANTS = List.of(
		merchant(106001, "경북대병원 외래", 16_700),
		merchant(106002, "동아약국", 8_900),
		merchant(106003, "CGV 대구", 15_000),
		merchant(106004, "메가박스 칠성점", 14_000),
		merchant(106005, "교보문고 대구점", 12_300),
		merchant(106006, "영풍문고 동대구점", 13_600),
		merchant(106007, "세탁특공대", 9_500),
		merchant(106008, "크린토피아 복현점", 6_000),
		merchant(106009, "필라테스 센터", 45_000),
		merchant(106010, "동네정형외과", 18_400)
	);

	private static final List<CardSeed> COMPLETED_MONTH_SEEDS = buildSeeds();

	public List<CardSeed> completedMonthSeeds() {
		return COMPLETED_MONTH_SEEDS;
	}

	private static List<CardSeed> buildSeeds() {
		List<SeedDraft> drafts = new ArrayList<>();
		int dayIndex = 0;
		dayIndex = addMonth(drafts, 2025, 12, 31, dayIndex);
		dayIndex = addMonth(drafts, 2026, 1, 31, dayIndex);
		dayIndex = addMonth(drafts, 2026, 2, 28, dayIndex);
		addMonth(drafts, 2026, 3, 25, dayIndex);
		drafts.sort(Comparator.comparing(SeedDraft::transactionDateTime));

		Map<String, Integer> daySequence = new HashMap<>();
		List<CardSeed> chronological = new ArrayList<>();
		for (SeedDraft draft : drafts) {
			String dayKey = draft.transactionDateTime().format(UNIQUE_NO_DAY_FORMAT);
			int sequence = daySequence.merge(dayKey, 1, Integer::sum);
			long uniqueNo = Long.parseLong(dayKey + String.format("%03d", sequence));
			chronological.add(new CardSeed(
				draft.transactionDateTime(),
				uniqueNo,
				draft.merchantId(),
				draft.merchantName(),
				draft.transactionBalance(),
				draft.purchaseStatus(),
				draft.transactionStatus()
			));
		}

		List<CardSeed> descending = new ArrayList<>(chronological);
		descending.sort(Comparator.comparing(CardSeed::transactionDateTime).reversed());
		return List.copyOf(descending);
	}

	private static int addMonth(List<SeedDraft> drafts, int year, int month, int lastDay, int startDayIndex) {
		for (int day = 1; day <= lastDay; day++) {
			addDayDrafts(drafts, LocalDate.of(year, month, day), startDayIndex + day - 1);
		}
		return startDayIndex + lastDay;
	}

	private static void addDayDrafts(List<SeedDraft> drafts, LocalDate date, int dayIndex) {
		drafts.add(transportDraft(date, dayIndex));
		drafts.add(primaryMealDraft(date, dayIndex));
		drafts.add(secondaryMealDraft(date, dayIndex));
		drafts.add(convenienceDraft(date, dayIndex));
		drafts.add(daytimeVariationDraft(date, dayIndex));
		drafts.add(nightVariationDraft(date, dayIndex));
	}

	private static SeedDraft transportDraft(LocalDate date, int dayIndex) {
		MerchantDraft merchant = pick(TRANSPORT_MERCHANTS, dayIndex, 3, date.getDayOfMonth());
		return draft(date, 8, 5 + (dayIndex % 20), merchant, PURCHASED_STATUS, NORMAL_STATUS, 150L * (dayIndex % 3));
	}

	private static SeedDraft primaryMealDraft(LocalDate date, int dayIndex) {
		MerchantDraft merchant = pick(FOOD_MERCHANTS, dayIndex, 5, 1);
		int hour = date.getDayOfMonth() % 2 == 0 ? 10 : 11;
		int minute = 10 + (dayIndex % 35);
		return draft(date, hour, minute, merchant, PURCHASED_STATUS, NORMAL_STATUS, 300L * (dayIndex % 4));
	}

	private static SeedDraft secondaryMealDraft(LocalDate date, int dayIndex) {
		MerchantDraft merchant = pick(FOOD_MERCHANTS, dayIndex, 7, 9);
		int hour = date.getDayOfMonth() % 3 == 0 ? 18 : 12;
		int minute = 15 + (dayIndex % 30);
		return draft(date, hour, minute, merchant, PURCHASED_STATUS, NORMAL_STATUS, 500L * (dayIndex % 5));
	}

	private static SeedDraft convenienceDraft(LocalDate date, int dayIndex) {
		MerchantDraft merchant = pick(CONVENIENCE_MERCHANTS, dayIndex, 2, 3);
		String purchaseStatus = dayIndex % 17 == 0 ? PENDING_STATUS : PURCHASED_STATUS;
		return draft(date, 15, 20 + (dayIndex % 25), merchant, purchaseStatus, NORMAL_STATUS, 200L * (dayIndex % 4));
	}

	private static SeedDraft daytimeVariationDraft(LocalDate date, int dayIndex) {
		return switch (dayIndex % 5) {
			case 0 -> draft(date, 16, 5 + (dayIndex % 25), pick(CAFE_MERCHANTS, dayIndex, 3, 0),
				PURCHASED_STATUS, NORMAL_STATUS, 250L * (dayIndex % 4));
			case 1 -> draft(date, 16, 12 + (dayIndex % 25), pick(CAFE_MERCHANTS, dayIndex, 5, 6),
				PURCHASED_STATUS, NORMAL_STATUS, 300L * (dayIndex % 4));
			case 2 -> draft(date, 17, 0 + (dayIndex % 20), pick(TRANSPORT_MERCHANTS, dayIndex, 4, 1),
				PURCHASED_STATUS, NORMAL_STATUS, 200L * (dayIndex % 3));
			case 3 -> draft(date, 17, 10 + (dayIndex % 20), pick(FOOD_MERCHANTS, dayIndex, 9, 3),
				PURCHASED_STATUS, NORMAL_STATUS, 500L * (dayIndex % 4));
			default -> draft(date, 16, 18 + (dayIndex % 25), pick(SHOPPING_MERCHANTS, dayIndex, 4, 2),
				PURCHASED_STATUS, NORMAL_STATUS, 700L * (dayIndex % 4));
		};
	}

	private static SeedDraft nightVariationDraft(LocalDate date, int dayIndex) {
		return switch (dayIndex % 10) {
			case 0 -> draft(date, 19, 5 + (dayIndex % 30), pick(FOOD_MERCHANTS, dayIndex, 11, 5),
				PURCHASED_STATUS, NORMAL_STATUS, 600L * (dayIndex % 5));
			case 1 -> draft(date, 19, 15 + (dayIndex % 25), pick(TRANSPORT_MERCHANTS, dayIndex, 5, 2),
				PURCHASED_STATUS, NORMAL_STATUS, 350L * (dayIndex % 3));
			case 2 -> draft(date, 19, 25 + (dayIndex % 20), pick(CAFE_MERCHANTS, dayIndex, 7, 4),
				PURCHASED_STATUS, NORMAL_STATUS, 200L * (dayIndex % 5));
			case 3 -> draft(date, 19, 35 + (dayIndex % 20), pick(FOOD_MERCHANTS, dayIndex, 13, 7),
				PURCHASED_STATUS, NORMAL_STATUS, 700L * (dayIndex % 4));
			case 4 -> draft(date, 20, 10 + (dayIndex % 20), pick(ONLINE_MERCHANTS, dayIndex, 3, 2),
				PENDING_STATUS, NORMAL_STATUS, 400L * (dayIndex % 4));
			case 5 -> draft(date, 20, 0 + (dayIndex % 20), pick(FOOD_MERCHANTS, dayIndex, 15, 1),
				PURCHASED_STATUS, NORMAL_STATUS, 600L * (dayIndex % 5));
			case 6 -> draft(date, 20, 5 + (dayIndex % 20), pick(LIFE_MERCHANTS, dayIndex, 4, 1),
				PURCHASED_STATUS, NORMAL_STATUS, 500L * (dayIndex % 3));
			case 7 -> draft(date, 20, 12 + (dayIndex % 20), pick(TRANSPORT_MERCHANTS, dayIndex, 6, 3),
				PURCHASED_STATUS, NORMAL_STATUS, 250L * (dayIndex % 3));
			case 8 -> draft(date, 20, 20 + (dayIndex % 20), pick(FOOD_MERCHANTS, dayIndex, 17, 9),
				PURCHASED_STATUS, NORMAL_STATUS, 500L * (dayIndex % 4));
			default -> draft(date, 20, 28 + (dayIndex % 20), pick(SHOPPING_MERCHANTS, dayIndex, 6, 1),
				PURCHASED_STATUS, dayIndex % 23 == 0 ? CANCELED_STATUS : NORMAL_STATUS, 800L * (dayIndex % 3));
		};
	}

	private static SeedDraft draft(
		LocalDate date,
		int hour,
		int minute,
		MerchantDraft merchant,
		String purchaseStatus,
		String transactionStatus,
		long amountDelta
	) {
		return new SeedDraft(
			LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), hour, minute),
			merchant.id(),
			merchant.name(),
			merchant.baseAmount() + amountDelta,
			purchaseStatus,
			transactionStatus
		);
	}

	private static MerchantDraft merchant(int id, String name, long baseAmount) {
		return new MerchantDraft(id, name, baseAmount);
	}

	private static MerchantDraft pick(List<MerchantDraft> merchants, int dayIndex, int multiplier, int offset) {
		return merchants.get(Math.floorMod(dayIndex * multiplier + offset, merchants.size()));
	}

	private record MerchantDraft(
		int id,
		String name,
		long baseAmount
	) {
	}

	private record SeedDraft(
		LocalDateTime transactionDateTime,
		int merchantId,
		String merchantName,
		long transactionBalance,
		String purchaseStatus,
		String transactionStatus
	) {
	}

	public record CardSeed(
		LocalDateTime transactionDateTime,
		Long transactionUniqueNo,
		Integer merchantId,
		String merchantName,
		Long transactionBalance,
		String purchaseStatus,
		String transactionStatus
	) {
	}
}
