package com.ssafy.amagetdone.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amazonaws.services.s3.AmazonS3;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.external-base-url=https://j14d101.p.ssafy.io/api")
class SwaggerConfigIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AmazonS3 amazonS3;

	@Test
	void apiDocsEndpointIsExposedUnderApiContextPath() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(content().encoding("UTF-8"))
			.andExpect(content().contentTypeCompatibleWith("application/json"))
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.servers[0].url").value("https://j14d101.p.ssafy.io/api"))
			.andExpect(jsonPath("$.info.title").value("\uBAA8\uC544 API"))
			.andExpect(jsonPath("$.info.description").value("\uBAA8\uC544 \uBC31\uC5D4\uB4DC API \uBB38\uC11C"));
	}

	@Test
	void swaggerUiEndpointIsExposedUnderApiContextPath() throws Exception {
		mockMvc.perform(apiGet("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"));
	}

	@Test
	void apiDocsDescribeS3TestEndpointsForLocalTesting() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(content().encoding("UTF-8"))
			.andExpect(jsonPath("$.paths['/test/s3/upload'].post.summary").value("S3 \uD30C\uC77C \uC5C5\uB85C\uB4DC \uD14C\uC2A4\uD2B8"))
			.andExpect(jsonPath("$.paths['/test/s3/upload'].post.tags[0]").value("S3 \uD14C\uC2A4\uD2B8"))
			.andExpect(jsonPath("$.paths['/test/s3'].delete.summary").value("S3 \uC5C5\uB85C\uB4DC \uD30C\uC77C \uC0AD\uC81C \uD14C\uC2A4\uD2B8"))
			.andExpect(jsonPath("$.paths['/test/s3'].delete.tags[0]").value("S3 \uD14C\uC2A4\uD2B8"));
	}

	@Test
	void apiDocsDescribeProfileAndPlanImageUploadEndpoints() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(content().encoding("UTF-8"))
			.andExpect(jsonPath(
				"$.paths['/v1/members/me/profile'].patch.requestBody.content['application/json'].schema['$ref']"
			).value("#/components/schemas/ProfileCompletionRequest"))
			.andExpect(jsonPath("$.components.schemas.ProfileCompletionRequest.properties.nickname.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.ProfileCompletionRequest.properties.email.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.ProfileCompletionRequest.properties.birthDate.type").value("string"))
			.andExpect(jsonPath("$.components.schemas.ProfileCompletionRequest.properties.hasFixedIncome.type").value("boolean"))
			.andExpect(jsonPath("$.paths['/v1/members/me/profile-image'].patch.summary").value("\uD504\uB85C\uD544 \uC774\uBBF8\uC9C0 \uC5C5\uB85C\uB4DC"))
			.andExpect(jsonPath("$.paths['/v1/members/me/profile-image'].patch.tags[0]").value("\uD68C\uC6D0 \uD504\uB85C\uD544 API"))
			.andExpect(jsonPath(
				"$.paths['/v1/members/me/profile-image'].patch.requestBody.content['multipart/form-data'].schema.properties.image.format"
			).value("binary"))
			.andExpect(jsonPath("$.paths['/v1/plans/{planId}/image'].patch.summary").value("\uBAA9\uD45C \uC774\uBBF8\uC9C0 \uC5C5\uB85C\uB4DC"))
			.andExpect(jsonPath("$.paths['/v1/plans/{planId}/image'].patch.tags[0]").value("\uBAA9\uD45C \uC774\uBBF8\uC9C0 API"))
			.andExpect(jsonPath(
				"$.paths['/v1/plans/{planId}/image'].patch.requestBody.content['multipart/form-data'].schema.properties.image.format"
			).value("binary"));
	}

	@Test
	void apiDocsExposeBearerSchemeAndKeepMockSocialAuthPublic() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
			.andExpect(jsonPath("$.paths['/transactions'].get.security[0].bearerAuth").isArray())
			.andExpect(jsonPath("$.paths['/v1/auth/social/test'].post.security").doesNotExist());
	}

	@Test
	void apiDocsMarkProtectedControllersAsBearerSecured() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/v1/accounts/me'].get.security[0].bearerAuth").isArray())
			.andExpect(jsonPath("$.paths['/v1/accounts/verification/requests'].post.security[0].bearerAuth").isArray())
			.andExpect(jsonPath("$.paths['/budgets/weeks/{yearWeek}/essential-expenses'].get.security[0].bearerAuth").isArray())
			.andExpect(jsonPath("$.paths['/v1/members/me/profile-image'].patch.security[0].bearerAuth").isArray())
			.andExpect(jsonPath("$.paths['/v1/plans/{planId}/image'].patch.security[0].bearerAuth").isArray())
			.andExpect(jsonPath("$.paths['/test/s3/upload'].post.security[0].bearerAuth").isArray())
			.andExpect(jsonPath("$.paths['/v1/accounts/me'].get.parameters").doesNotExist());
	}

	@Test
	void apiDocsKeepMockSocialAuthPublicAndProtectMemberProfile() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/v1/auth/social/test'].post.summary").exists())
			.andExpect(jsonPath("$.paths['/v1/auth/social/test'].post.security").doesNotExist())
			.andExpect(jsonPath("$.paths['/v1/members/me/profile'].patch.security[0].bearerAuth").isArray());
	}

	private MockHttpServletRequestBuilder apiGet(String path) {
		return get("/api" + path).contextPath("/api");
	}
}
