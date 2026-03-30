package com.ssafy.amagetdone.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI moreOpenApi(@Value("${app.external-base-url:}") String externalBaseUrl) {
		OpenAPI openApi = new OpenAPI()
			.components(new Components().addSecuritySchemes(
				"bearerAuth",
				new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
			))
			.info(new Info()
				.title("\uBAA8\uC544 API")
				.description("\uBAA8\uC544 \uBC31\uC5D4\uB4DC API \uBB38\uC11C")
				.version("v1"));

		if (!externalBaseUrl.isBlank()) {
			openApi.servers(List.of(new Server()
				.url(externalBaseUrl)
				.description("External server url")));
		}

		return openApi;
	}
}
