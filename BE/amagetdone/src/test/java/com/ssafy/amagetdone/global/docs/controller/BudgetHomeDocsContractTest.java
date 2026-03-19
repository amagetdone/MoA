package com.ssafy.amagetdone.global.docs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetHomeDto;
import com.ssafy.amagetdone.global.docs.schema.BudgetHomeApiResponseSchema;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.junit.jupiter.api.Test;

class BudgetHomeDocsContractTest {

	@Test
	void budgetHomeEndpointUsesExplicitResponseWrapperSchema() throws NoSuchMethodException {
		Method method = BudgetDocsController.class.getMethod("getBudgetHome", AuthenticatedMember.class);
		ApiResponses responses = method.getAnnotation(ApiResponses.class);

		assertNotNull(responses);

		io.swagger.v3.oas.annotations.responses.ApiResponse okResponse = Arrays.stream(responses.value())
			.filter(response -> "200".equals(response.responseCode()))
			.findFirst()
			.orElseThrow();

		Content[] content = okResponse.content();
		assertEquals(1, content.length);
		assertEquals(BudgetHomeApiResponseSchema.class, content[0].schema().implementation());
	}

	@Test
	void budgetHomeResponseSchemaNamesRemainStable() {
		Schema apiResponseSchema = BudgetHomeApiResponseSchema.class.getAnnotation(Schema.class);
		Schema budgetHomeSchema = BudgetHomeDto.Response.class.getAnnotation(Schema.class);
		Schema wrapperDataSchema = BudgetHomeApiResponseSchema.BudgetHomeResponseSchema.class.getAnnotation(Schema.class);

		assertNotNull(apiResponseSchema);
		assertNotNull(budgetHomeSchema);
		assertNotNull(wrapperDataSchema);
		assertEquals("BudgetHomeApiResponse", apiResponseSchema.name());
		assertEquals("BudgetHomeResponse", budgetHomeSchema.name());
		assertEquals("BudgetHomeResponse", wrapperDataSchema.name());
	}

	@Test
	void budgetHomeWrapperContainsExpectedSections() {
		Map<String, Class<?>> components = Arrays.stream(BudgetHomeApiResponseSchema.BudgetHomeResponseSchema.class.getRecordComponents())
			.collect(Collectors.toMap(RecordComponent::getName, RecordComponent::getType));

		assertEquals(BudgetHomeApiResponseSchema.BudgetHomeResponseSchema.class, dataComponent().getType());
		assertEquals(4, components.size());
		assertEquals(BudgetHomeApiResponseSchema.FreeSpendingSchema.class, components.get("freeSpending"));
		assertEquals(BudgetHomeApiResponseSchema.WeeklySavingSchema.class, components.get("weeklySaving"));
		assertEquals(BudgetHomeApiResponseSchema.AssetSummarySchema.class, components.get("assetSummary"));
		assertEquals(java.util.List.class, components.get("plans"));
	}

	private RecordComponent dataComponent() {
		return Arrays.stream(BudgetHomeApiResponseSchema.class.getRecordComponents())
			.filter(component -> "data".equals(component.getName()))
			.findFirst()
			.orElseThrow();
	}
}
