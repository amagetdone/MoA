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
	private static final int MARCH_2026_SEED_END_DAY = 29;
	private static final String PURCHASED_STATUS = "\uB9E4\uC785";
	private static final String PENDING_STATUS = "\uBBF8\uB9E4\uC785";
	private static final String NORMAL_STATUS = "\uC815\uC0C1";
	private static final String CANCELED_STATUS = "\uCDE8\uC18C";

	private static final List<MerchantDraft> TRANSPORT_MERCHANTS = List.of(
		merchant(16695, "\uCE74\uCE74\uC624\uD0DD\uC2DC", 9_300),
		merchant(16696, "\uCF54\uB808\uC77C", 12_800),
		merchant(16697, "\uD55C\uAD6D\uCCA0\uB3C4\uACF5\uC0AC", 18_900),
		merchant(16689, "\uD604\uB300\uC624\uC77C\uBC45\uD06C", 45_000),
		merchant(16690, "SK\uC5D4\uD06C\uB9B0", 47_000),
		merchant(16691, "GS\uCE7C\uD14D\uC2A4", 46_500),
		merchant(23586, "\uB3D9\uAD6D\uC8FC\uC720\uC18C", 44_000),
		merchant(23678, "SK\uC608\uC77C\uC8FC\uC720\uC18C", 43_500),
		merchant(23482, "\uACBD\uAE30\uB80C\uD2B8\uCE74", 38_000),
		merchant(23130, "\uC528\uC5D0\uC5B4\uD56D\uACF5\uC5EC\uD589\uC0AC", 29_000)
	);

	private static final List<MerchantDraft> FOOD_MERCHANTS = List.of(
		merchant(23040, "\uBD04\uB0B4\uCE58\uD0A8\uC564\uB5A1\uBCF6\uC774", 17_900),
		merchant(23241, "\uB124\uB124\uCE58\uD0A8.\uD53C\uC790", 18_500),
		merchant(23338, "\uD669\uC81C\uCE58\uD0A8", 19_200),
		merchant(23404, "\uAD50\uCD0C\uCE58\uD0A8", 18_700),
		merchant(23056, "\uAE50\uC21C\uC774\uC2DD\uB2F9", 8_900),
		merchant(23087, "\uC11C\uC6B8\uC2DD\uB2F9", 9_400),
		merchant(23053, "\uC624\uC5D4\uAD6D\uC218", 7_800),
		merchant(23126, "\uC774\uCE35\uCE7C\uAD6D\uC218", 8_700),
		merchant(23151, "\uB2E4\uB274\uBD84\uC2DD&\uC138\uACBD\uB300\uD3EC\uC9D1", 6_900),
		merchant(23118, "\uBD80\uC624\uB098\uD53C\uC790\uAC15\uB989\uC810", 22_000),
		merchant(23077, "\uC870\uC120\uB3FC\uC9C0\uAC08\uBE44\uB2E8\uAD6C\uC120\uC544", 19_000),
		merchant(23082, "\uCD08\uC6D0\uAC08\uBE44", 21_000),
		merchant(23103, "\uB300\uC6B0\uB2ED\uAC08\uBE44", 16_500),
		merchant(23094, "\uC6D0\uC870\uD55C\uCE58\uD68C", 23_500),
		merchant(23105, "\uBF08\uB300\uC788\uB294\uC9EC\uBF55", 11_500),
		merchant(23133, "\uB0A9\uB3CC\uCD94\uC5B4\uD0D5", 10_500),
		merchant(23132, "\uC5C4\uB9C8\uAC00\uCC28\uB824\uC8FC\uB294\uBC25\uC0C1", 12_500),
		merchant(23128, "\uC544\uC7AC\uB124\uAD6C\uC774", 14_500),
		merchant(23084, "\uC11C\uC9C4\uBD88\uB2ED\uBC1C", 17_200),
		merchant(23149, "\uB3D9\uC218\uB9AC\uB9C9\uAD6D\uC218", 9_900)
	);

	private static final List<MerchantDraft> CONVENIENCE_MERCHANTS = List.of(
		merchant(24924, "GS25\uAC15\uB989", 2_300),
		merchant(25185, "GS25\uCD98\uCC9C", 2_700),
		merchant(26042, "GS25\uC601\uC6D4", 3_100),
		merchant(23140, "\uC528\uC720\uAC15\uB989\uBBF8\uB514\uC5B4\uCD0C5\uB2E8\uC9C0\uC810", 2_600),
		merchant(23174, "\uC528\uC720\uAE40\uC720\uC815\uC5ED\uC810", 2_400),
		merchant(23896, "\uC528\uC720\uCD98\uCC9C", 2_900),
		merchant(23141, "\uC138\uBE10\uC77C\uB808\uBE10\uCD98\uCC9C\uC19F\uBC1C\uAE38\uC810", 2_700),
		merchant(23611, "\uC138\uBE10\uC77C\uB808\uBE10\uAC15\uB989\uC785\uC554", 2_800),
		merchant(24833, "\uC138\uBE10\uC77C\uB808\uBE10\uC0BC\uCC99", 3_200),
		merchant(16709, "24\uC2DC \uC544\uC774\uC2A4\uD06C\uB9BC \uD560\uC778\uC810", 4_900)
	);

	private static final List<MerchantDraft> CAFE_MERCHANTS = List.of(
		merchant(16707, "\uC2A4\uD0C0\uBC85\uC2A4 \uC11C\uC6B8\uC5ED\uC810", 5_500),
		merchant(23134, "\uAC15\uB989\uB2F9\uCEE4\uD53C\uCF69\uBE75\uC548\uBAA9", 6_200),
		merchant(23159, "\uCEE4\uD53C\uD074\uB7FD", 4_700),
		merchant(23223, "\uBA54\uAC00\uC5E0\uC9C0\uC528\uCEE4\uD53C\uC6D0\uC8FC\uD765\uC5C5", 3_900),
		merchant(23278, "\uC624\uB4DC\uCEE4\uD53C", 5_100),
		merchant(23469, "\uCEE4\uD53C\uD1A1", 4_300),
		merchant(23583, "\uCEE4\uD53C\uACE0", 4_800),
		merchant(16698, "\uD0C0\uC694\uD0A4\uC988\uCE74\uD398 \uB300\uC804\uB454\uC0B0\uC810", 12_000),
		merchant(23129, "\uCE74\uD398\uC544\uB97C", 6_400),
		merchant(23198, "\uCE74\uD398\uC5D8\uB791", 5_900)
	);

	private static final List<MerchantDraft> ONLINE_MERCHANTS = List.of(
		merchant(16706, "Temu", 24_300),
		merchant(16704, "\uC54C\uB9AC\uC775\uC2A4\uD504\uB808\uC2A4", 19_800),
		merchant(16705, "\uC544\uB9C8\uC874 \uCF54\uB9AC\uC544", 21_500),
		merchant(28603, "\uC544\uB9C8\uC874", 23_000),
		merchant(23054, "\uCCAD\uD638\uB098\uC774\uC2A4\uB80C\uD0C8", 17_000),
		merchant(24482, "\uC6D0\uC8FC\uB80C\uD0C8", 16_500),
		merchant(28203, "\uCF54\uC6E8\uC774\uB80C\uD0C8\uC6D0\uC8FC\uC810", 18_900),
		merchant(29414, "\uAC15\uB989\uB80C\uD0C8", 14_900),
		merchant(30383, "\uCCAD\uD638\uB098\uC774\uC2A4\uB80C\uD0C8\uAC15\uB989\uC810", 16_200),
		merchant(31556, "\uB118\uBC84\uC6D0\uB80C\uD0C8\uC0F5", 13_500)
	);

	private static final List<MerchantDraft> SHOPPING_MERCHANTS = List.of(
		merchant(16692, "\uC774\uB9C8\uD2B8 \uC740\uD3C9\uC810", 28_500),
		merchant(16693, "\uB86F\uB370\uB9C8\uD2B8 \uAC15\uBCC0\uC810", 31_800),
		merchant(16694, "\uCF54\uC2A4\uD2B8\uCF54", 34_000),
		merchant(23101, "\uC774\uB2C8\uC2A4\uD504\uB9AC\uC774\uB9C8\uD2B8\uAC15\uB989\uADF8\uB9B0", 18_900),
		merchant(23123, "\uBC14\uC6B8\uB9AC\uB098\uD328\uC158", 22_500),
		merchant(23097, "\uC544\uCE74\uB370\uBBF8\uC548\uACBD\uC6D0", 16_400),
		merchant(23108, "\uD64D\uBA54\uD654\uC7A5\uD488", 14_200),
		merchant(23164, "\uD0B9\uBAB0", 19_800),
		merchant(27873, "\uD0D1\uD150\uD0A4\uC988\uB86F\uB370\uB9C8\uD2B8", 27_000),
		merchant(30139, "\uC774\uB2C8\uC2A4\uD504\uB9AC\uCD98\uCC9C\uADF8\uB9B0\uC810", 17_400)
	);

	private static final List<MerchantDraft> LIFE_MERCHANTS = List.of(
		merchant(23050, "\uC724\uC2AC\uC57D\uAD6D", 11_500),
		merchant(23059, "\uAE40\uC6D0\uB798\uCE58\uACFC\uC758\uC6D0", 19_000),
		merchant(23060, "\uBC15\uCC2C\uC6B0365\uC758\uC6D0", 18_500),
		merchant(23048, "\uC601\uC7AC\uBBF8\uC6A9\uC2E4", 15_000),
		merchant(23049, "\uC774\uBE0CPC\uBC29", 8_900),
		merchant(23043, "\uBCF4\uB77C\uD39C\uC158", 26_000),
		merchant(23041, "\uB2E4\uBE48\uCE58\uBAA8\uD154", 42_000),
		merchant(23086, "\uD5EC\uC2A4", 14_000),
		merchant(23066, "\uCE58\uC545\uCC1C\uC9C8\uC0AC\uC6B0\uB098", 13_000),
		merchant(23163, "\uD504\uB77C\uC790\uAC74\uAC15\uC57D\uAD6D", 10_500)
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
		addMonth(drafts, 2026, 3, MARCH_2026_SEED_END_DAY, dayIndex);
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
