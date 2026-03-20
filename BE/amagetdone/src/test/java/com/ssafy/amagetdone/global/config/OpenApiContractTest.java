package com.ssafy.amagetdone.global.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.budget.api.dto.BudgetHomeDto;
import com.ssafy.amagetdone.global.docs.controller.BudgetDocsController;
import com.ssafy.amagetdone.global.docs.schema.BudgetHomeApiResponseSchema;
import com.ssafy.amagetdone.global.security.AuthenticatedMember;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.junit.jupiter.api.Test;

class OpenApiContractTest {

	private final SwaggerConfig swaggerConfig = new SwaggerConfig();

	@Test
	void addsBearerAuthSecurityScheme() {
		OpenAPI openApi = swaggerConfig.moreOpenApi("");
		SecurityScheme bearerAuth = openApi.getComponents().getSecuritySchemes().get("bearerAuth");

		assertNotNull(bearerAuth);
		assertEquals(SecurityScheme.Type.HTTP, bearerAuth.getType());
		assertEquals("bearer", bearerAuth.getScheme());
		assertEquals("JWT", bearerAuth.getBearerFormat());
	}

	@Test
	void omitsServersWhenExternalBaseUrlIsBlank() {
		OpenAPI openApi = swaggerConfig.moreOpenApi("");

		assertTrue(openApi.getServers() == null || openApi.getServers().isEmpty());
	}

	@Test
	void addsConfiguredExternalBaseUrlAsServer() {
		OpenAPI openApi = swaggerConfig.moreOpenApi("https://j14d101.p.ssafy.io/api");

		assertNotNull(openApi.getServers());
		assertEquals(1, openApi.getServers().size());
		assertEquals("https://j14d101.p.ssafy.io/api", openApi.getServers().get(0).getUrl());
	}

	@Test
	void budgetHomeEndpointUsesExplicitResponseWrapperSchema() throws Exception {
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
	void budgetHomeSchemaNamesAndSectionsRemainStable() {
		Schema apiResponseSchema = BudgetHomeApiResponseSchema.class.getAnnotation(Schema.class);
		Schema budgetHomeSchema = BudgetHomeDto.Response.class.getAnnotation(Schema.class);
		Schema wrapperDataSchema = BudgetHomeApiResponseSchema.BudgetHomeResponseSchema.class.getAnnotation(Schema.class);
		Map<String, Class<?>> components = Arrays.stream(BudgetHomeApiResponseSchema.BudgetHomeResponseSchema.class.getRecordComponents())
			.collect(Collectors.toMap(RecordComponent::getName, RecordComponent::getType));
		Map<String, Class<?>> assetSummaryComponents = Arrays.stream(BudgetHomeApiResponseSchema.AssetSummarySchema.class.getRecordComponents())
			.collect(Collectors.toMap(RecordComponent::getName, RecordComponent::getType));

		assertNotNull(apiResponseSchema);
		assertNotNull(budgetHomeSchema);
		assertNotNull(wrapperDataSchema);
		assertEquals("BudgetHomeApiResponse", apiResponseSchema.name());
		assertEquals("BudgetHomeResponse", budgetHomeSchema.name());
		assertEquals("BudgetHomeResponse", wrapperDataSchema.name());
		assertEquals(BudgetHomeApiResponseSchema.BudgetHomeResponseSchema.class, dataComponent().getType());
		assertEquals(4, components.size());
		assertEquals(BudgetHomeApiResponseSchema.FreeSpendingSchema.class, components.get("freeSpending"));
		assertEquals(BudgetHomeApiResponseSchema.WeeklySavingSchema.class, components.get("weeklySaving"));
		assertEquals(BudgetHomeApiResponseSchema.AssetSummarySchema.class, components.get("assetSummary"));
		assertEquals(java.util.List.class, components.get("plans"));
		assertEquals(2, assetSummaryComponents.size());
		assertEquals(long.class, assetSummaryComponents.get("totalAssetAmount"));
		assertEquals(java.util.List.class, assetSummaryComponents.get("accounts"));
	}

	private RecordComponent dataComponent() {
		return Arrays.stream(BudgetHomeApiResponseSchema.class.getRecordComponents())
			.filter(component -> "data".equals(component.getName()))
			.findFirst()
			.orElseThrow();
	}
}
