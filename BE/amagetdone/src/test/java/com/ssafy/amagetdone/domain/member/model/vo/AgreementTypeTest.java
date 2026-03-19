package com.ssafy.amagetdone.domain.member.model.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class AgreementTypeTest {

	@Test
	void agreementTypesFollowOperationalConsentStructure() {
		List<String> agreementTypeNames = Arrays.stream(AgreementType.values())
			.map(Enum::name)
			.toList();

		assertEquals(
			List.of(
				"TERMS_OF_SERVICE",
				"PERSONAL_INFO_COLLECTION_USE",
				"FINANCIAL_INFO_QUERY_USE",
				"MARKETING_INFO_RECEIVE"
			),
			agreementTypeNames
		);
	}
}
