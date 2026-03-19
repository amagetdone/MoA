package com.ssafy.amagetdone.domain.account.support.util;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class CardRealTransactionScenario {

	private static final List<CardSeed> COMPLETED_MONTH_SEEDS = List.of(
		"2026-03-20T19:55:42|20358114|1545119|\ud0c0\uc774\uc9ec\ubf55 \uc778\ub3d9\uc810|59300|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-19T22:18:11|19358113|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|32100|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-18T20:22:45|18358112|4402386|(\uc8fc) \ub354\uc388\ud14c\ud06c \ubd80\ud3c9\ubaa8\ub2e4\uc810|45800|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-16T20:31:18|16358111|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|39000|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-15T16:12:57|15358110|2715766|\ucf54\ub808\uc77c\uc720\ud1b5(\uc8fc)(\uad6c\ubbf8\uc5ed)|7600|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-14T18:17:26|14358109|4353134|(\uc8fc)\uc7a5\ubcf4\uace0\uc2dd\uc790\uc7ac\ub9c8\ud2b8(\uad6c\ubbf8|21900|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-12T12:25:11|12358108|2457601|\ub9d8\uc2a4\ud130\uce58 \uad6c\ubbf8\uc778\ub3d9\uc810|17300|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-11T11:06:18|11358107|1774643|\ub354\ubca4\ud2f0\uad6c\ubbf8\uc0bc\uc131\uc810|2900|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-10T19:08:45|10358106|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|24500|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-07T19:52:31|09358105|2180844|\ud55c\ub3c8\ucc38\uc22f\uaf2c\uae30\uc778\ub3d9\uc810|49800|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-05T22:04:12|08358104|1630006|\uce74\uce74\uc624T\uc77c\ubc18\ud0dd\uc2dc_0|5000|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-05T21:57:44|07358103|2189915|\uce74\uce74\uc624T\ud0dd\uc2dc_\uac00\uc2b9\uc778|4900|\ubbf8\ub9e4\uc785|\ucde8\uc18c",
		"2026-03-02T18:37:41|06358102|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|12900|\ub9e4\uc785|\uc815\uc0c1",
		"2026-03-01T14:48:19|05358101|2020230|\ub300\uad6c\uc9c0\ud558\ucca0_\uc720\ud398\uc774|1500|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-25T21:04:32|20258112|3103952|\ubc30\ubbfc\ud074\ub7fd_\uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|15900|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-24T07:31:45|19258111|2020230|\ub300\uad6c\uc9c0\ud558\ucca0_\uc720\ud398\uc774|1500|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-21T19:12:18|18258110|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|21000|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-20T13:09:31|17258109|4402386|(\uc8fc) \ub354\uc388\ud14c\ud06c \ubd80\ud3c9\ubaa8\ub2e4\uc810|41200|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-16T20:34:55|16258108|2754177|\uc77c\ud488\uc591\ud3c9\ud574\uc7a5\uad6d\uc778\ub3d9\uc810|26400|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-15T14:18:12|15258107|1404875|\uc2a4\ud0c0\ubc85\uc2a4\ucf54\ub9ac\uc544|6300|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-11T18:51:33|14258106|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|23600|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-10T12:02:44|13258105|2181103|\ub86f\ub370\ub9ac\uc544\uad6c\ubbf8\uc778\ub3d9\uc810|17800|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-07T18:47:06|12258104|4353134|(\uc8fc)\uc7a5\ubcf4\uace0\uc2dd\uc790\uc7ac\ub9c8\ud2b8(\uad6c\ubbf8|20300|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-06T21:22:16|11258103|1630006|\uce74\uce74\uc624T\uc77c\ubc18\ud0dd\uc2dc_0|5000|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-06T21:18:44|10258102|2189915|\uce74\uce74\uc624T\ud0dd\uc2dc_\uac00\uc2b9\uc778|5000|\ubbf8\ub9e4\uc785|\ucde8\uc18c",
		"2026-02-05T20:31:21|09258101|4889604|\uad6c\uae00\ud398\uc774\uba3c\ud2b8\ucf54\ub9ac\uc544 \uc720\ud55c\ud68c\uc0ac|9500|\ub9e4\uc785|\uc815\uc0c1",
		"2026-02-02T19:14:51|08258100|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|12100|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-25T19:41:22|20158112|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|42770|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-24T08:17:03|19158111|2020230|\ub300\uad6c\uc9c0\ud558\ucca0_\uc720\ud398\uc774|1500|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-20T20:18:42|18158110|1545119|\ud0c0\uc774\uc9ec\ubf55 \uc778\ub3d9\uc810|48200|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-20T13:04:57|17158109|2395485|\ud22c\uc378\ud50c\ub808\uc774\uc2a4 \uc778\ub3d9\uc810|6100|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-18T18:25:10|16158108|4353134|(\uc8fc)\uc7a5\ubcf4\uace0\uc2dd\uc790\uc7ac\ub9c8\ud2b8(\uad6c\ubbf8|21900|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-17T11:40:31|15158107|1774643|\ub354\ubca4\ud2f0\uad6c\ubbf8\uc0bc\uc131\uc810|2900|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-14T20:22:55|14158106|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|38900|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-13T19:05:43|13158105|2754177|\uc77c\ud488\uc591\ud3c9\ud574\uc7a5\uad6d\uc778\ub3d9\uc810|16800|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-10T11:42:10|12158104|2181103|\ub86f\ub370\ub9ac\uc544\uad6c\ubbf8\uc778\ub3d9\uc810|15900|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-07T08:35:11|11158103|2233223|\uce74\uce74\uc624_\ud0dd\uc2dc_\uc120\uc2b9\uc778|5100|\ubbf8\ub9e4\uc785|\ucde8\uc18c",
		"2026-01-07T08:29:48|10158102|878156|\uce74\uce74\uc624_\ud0dd\uc2dc9|5000|\ub9e4\uc785|\uc815\uc0c1",
		"2026-01-06T19:44:02|09158101|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|12500|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-25T12:25:19|00371384|47437|ORACLE  SINGAPORE        SINGAPORE    SG|160693|\ubbf8\ub9e4\uc785|\ucde8\uc18c",
		"2025-12-24T15:15:51|37293724|2020230|\ub300\uad6c\uc9c0\ud558\ucca0_\uc720\ud398\uc774|1500|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-23T16:32:10|39086740|2176139|\uc9f1\uc624\ub77d\uc2e4 \ud0dc\uc6553\ud638\uc810|1000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-23T16:31:12|22093127|2176139|\uc9f1\uc624\ub77d\uc2e4 \ud0dc\uc6553\ud638\uc810|1000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-23T16:29:34|08093450|2176139|\uc9f1\uc624\ub77d\uc2e4 \ud0dc\uc6553\ud638\uc810|1000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-23T16:28:29|32096144|2176139|\uc9f1\uc624\ub77d\uc2e4 \ud0dc\uc6553\ud638\uc810|1000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-22T18:21:43|21066381|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|36941|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-22T11:04:17|11048206|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|10500|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-22T09:50:58|29036473|878156|\uce74\uce74\uc624_\ud0dd\uc2dc9|5000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-22T09:42:23|10040870|2233223|\uce74\uce74\uc624_\ud0dd\uc2dc_\uc120\uc2b9\uc778|5100|\ubbf8\ub9e4\uc785|\ucde8\uc18c",
		"2025-12-22T09:19:53|15409701|3103952|\ubc30\ubbfc\ud074\ub7fd_\uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|13990|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-21T14:42:49|56684504|2339876|\uacbd\ubd81\uc2dc\ub0b4\ubc84\uc2a4_\ub9c8\uc774\ube44|3400|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-20T13:17:36|02986429|2395485|\ud22c\uc378\ud50c\ub808\uc774\uc2a4 \uc778\ub3d9\uc810|1000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-20T12:50:18|13979931|1545119|\ud0c0\uc774\uc9ec\ubf55 \uc778\ub3d9\uc810|90000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-19T21:29:53|02970419|2754177|\uc77c\ud488\uc591\ud3c9\ud574\uc7a5\uad6d\uc778\ub3d9\uc810|16000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-19T20:16:58|44709166|2457601|\ub9d8\uc2a4\ud130\uce58 \uad6c\ubbf8\uc778\ub3d9\uc810|14600|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-19T11:40:13|35935341|1774643|\ub354\ubca4\ud2f0\uad6c\ubbf8\uc0bc\uc131\uc810|2850|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-18T21:17:34|38927783|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|20535|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-18T13:54:23|20058208|2093040|\ud2f0\uba38\ub2c8 \ub300\uc804\uc9c0\ud558\ucca0|1550|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-17T20:45:39|14896646|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|62000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-17T10:42:04|37874657|2548399|(\uc8fc)\ube44\ubc14\ub9ac\ud37c\ube14\ub9ac\uce74|5900|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-16T18:29:09|14409101|1580932|(\uc8fc)\uc544\uc131\ub2e4\uc774\uc18c|5000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-16T14:13:49|13409101|4402386|(\uc8fc) \ub354\uc36c\ud14c\ud06c \ubd80\ud3c9\ubaa8\ub2e4\uc810|34800|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-16T10:45:11|12409101|2715766|\ucf54\ub808\uc77c\uc720\ud1b5(\uc8fc)(\uad6c\ubbf8\uc5ed)|4800|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-16T10:35:54|11409101|2715766|\ucf54\ub808\uc77c\uc720\ud1b5(\uc8fc)(\uad6c\ubbf8\uc5ed)|9000|\ubbf8\ub9e4\uc785|\ucde8\uc18c",
		"2025-12-14T19:56:01|00409901|4353134|(\uc8fc)\uc7a5\ubcf4\uace0\uc2dd\uc790\uc7ac\ub9c8\ud2b8(\uad6c\ubbf8|8240|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-13T21:46:44|10741882|1630006|\uce74\uce74\uc624T\uc77c\ubc18\ud0dd\uc2dc_0|5000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-13T21:38:51|12751482|2189915|\uce74\uce74\uc624T\ud0dd\uc2dc_\uac00\uc2b9\uc778|4700|\ubbf8\ub9e4\uc785|\ucde8\uc18c",
		"2025-12-13T11:20:15|09408801|3124007|\uc8fc\uc2dd\ud68c\uc0ac \uc6b0\uc544\ud55c\ud615\uc81c\ub4e4|23800|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-12T23:40:23|08408701|2180844|\ud55c\ub3c8\ucc38\uc22f\uaf2c\uae30\uc778\ub3d9\uc810|260100|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-11T14:13:35|07408601|1404875|\uc2a4\ud0c0\ubc85\uc2a4\ucf54\ub9ac\uc544|10000|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-10T18:11:00|06408501|2181103|\ub86f\ub370\ub9ac\uc544\uad6c\ubbf8\uc778\ub3d9\uc810|17800|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-09T20:31:20|42337480|4889604|\uad6c\uae00\ud398\uc774\uba3c\ud2b8\ucf54\ub9ac\uc544 \uc720\ud55c\ud68c\uc0ac|9500|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-09T18:06:15|01602650|468867|\uadf8\ub9b0\uc57d\uad6d|3500|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-08T20:20:58|05408301|1390150|\uce74\ud398\ub85c\uc0ac\uc778\ub3d9\uc810|4500|\ub9e4\uc785|\uc815\uc0c1",
		"2025-12-08T20:17:39|04408301|5106065|\uac04\ubc14\uc9c0\uc5f0\ud0c4\uace0\ucd94\uc7a5\ubd88\uace0\uae30\uc778\ub3d9\uc810|73500|\ub9e4\uc785|\uc815\uc0c1"
	).stream()
		.map(CardRealTransactionScenario::parse)
		.toList();

	public List<CardSeed> completedMonthSeeds() {
		return COMPLETED_MONTH_SEEDS;
	}

	private static CardSeed parse(String raw) {
		String[] tokens = raw.split("\\|", -1);
		return new CardSeed(
			LocalDateTime.parse(tokens[0]),
			Long.parseLong(tokens[1]),
			Integer.parseInt(tokens[2]),
			tokens[3],
			Long.parseLong(tokens[4]),
			tokens[5],
			tokens[6]
		);
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
