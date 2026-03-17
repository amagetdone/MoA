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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerConfigIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AmazonS3 amazonS3;

	@Test
	void apiDocsEndpointIsExposedUnderApiContextPath() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("application/json"))
			.andExpect(jsonPath("$.openapi").exists());
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
			.andExpect(jsonPath("$.paths['/test/s3/upload'].post.summary").value("Upload a file to S3"))
			.andExpect(jsonPath("$.paths['/test/s3/upload'].post.tags[0]").value("S3 Test"))
			.andExpect(jsonPath("$.paths['/test/s3'].delete.summary").value("Delete an uploaded S3 file"))
			.andExpect(jsonPath("$.paths['/test/s3'].delete.tags[0]").value("S3 Test"))
			.andExpect(jsonPath("$.paths['/test/budget/weeks/seed'].post.summary").value("Seed budget weeks"))
			.andExpect(jsonPath("$.paths['/test/budget/weeks/seed'].post.tags[0]").value("Budget Test"))
			.andExpect(jsonPath("$.paths['/test/budget/weeks/seed'].post.security[0].bearerAuth").exists());
	}

	@Test
	void apiDocsDescribeProfileAndPlanImageUploadEndpoints() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/v1/members/me/profile-image'].patch.summary").value("프로필 이미지 업로드"))
			.andExpect(jsonPath("$.paths['/v1/members/me/profile-image'].patch.tags[0]").value("Member Profile API"))
			.andExpect(jsonPath(
				"$.paths['/v1/members/me/profile-image'].patch.requestBody.content['multipart/form-data'].schema.properties.image.format"
			).value("binary"))
			.andExpect(jsonPath("$.paths['/v1/plans/{planId}/image'].patch.summary").value("목표 이미지 업로드"))
			.andExpect(jsonPath("$.paths['/v1/plans/{planId}/image'].patch.tags[0]").value("Plan Image API"))
			.andExpect(jsonPath(
				"$.paths['/v1/plans/{planId}/image'].patch.requestBody.content['multipart/form-data'].schema.properties.image.format"
			).value("binary"));
	}

	@Test
	void apiDocsDescribePlanCreationEndpoint() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/v1/plans'].post.summary").value("목표 생성"))
			.andExpect(jsonPath("$.paths['/v1/plans'].post.tags[0]").value("Plan API"))
			.andExpect(jsonPath("$.paths['/v1/plans'].post.security[0].bearerAuth").exists())
			.andExpect(jsonPath("$.components.schemas.PlanCreateRequest.properties.dateRangeValid").doesNotExist())
			.andExpect(jsonPath("$.components.schemas.PlanCreateRequest.example").exists())
			.andExpect(jsonPath("$.components.schemas.BudgetWeekSeedRequest.example").exists());
	}

	@Test
	void apiDocsExposeBearerAuthenticationScheme() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
	}

	@Test
	void apiDocsMarkAuthenticatedUploadEndpointsWithBearerSecurity() throws Exception {
		mockMvc.perform(apiGet("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/v1/members/me/profile-image'].patch.security[0].bearerAuth").exists())
			.andExpect(jsonPath("$.paths['/v1/plans/{planId}/image'].patch.security[0].bearerAuth").exists());
	}

	private MockHttpServletRequestBuilder apiGet(String path) {
		return get("/api" + path).contextPath("/api");
	}
}
