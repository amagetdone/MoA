package com.ssafy.amagetdone.domain.mcp.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import com.ssafy.amagetdone.domain.mcp.application.service.AssetMcpQueryAdapter;
import com.ssafy.amagetdone.domain.mcp.application.service.BudgetMcpQueryAdapter;
import com.ssafy.amagetdone.domain.mcp.application.service.TransactionMcpQueryAdapter;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class McpFinanceToolConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(McpFinanceToolConfiguration.class, TestAdapterConfiguration.class);

	@Test
	void doesNotRegisterToolCallbackProviderWhenMcpIsDisabled() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(ToolCallbackProvider.class));
	}

	@Test
	void registersAllFinanceReadToolsWhenMcpIsEnabled() {
		contextRunner
			.withPropertyValues("mcp.finance.enabled=true")
			.run(context -> {
				assertThat(context).hasSingleBean(ToolCallbackProvider.class);
				assertThat(context.getBean(ToolCallbackProvider.class).getToolCallbacks()).hasSize(20);
				assertThat(Arrays.stream(context.getBean(ToolCallbackProvider.class).getToolCallbacks()))
					.allSatisfy(tool -> {
						assertThat(tool.getToolDefinition().description()).contains("Use when:");
						assertThat(tool.getToolDefinition().description()).contains("Best for:");
					});
				String schema = Arrays.stream(context.getBean(ToolCallbackProvider.class).getToolCallbacks())
					.filter(tool -> tool.getToolDefinition().name().equals("get_budget_home"))
					.findFirst()
					.orElseThrow()
					.getToolDefinition()
					.inputSchema();
				assertThat(schema).contains("\"user_id\"");
			});
	}

	@Configuration
	static class TestAdapterConfiguration {

		@Bean
		BudgetMcpQueryAdapter budgetMcpQueryAdapter() {
			return new BudgetMcpQueryAdapter(null, null, null, null, null, null, null, null, null);
		}

		@Bean
		AssetMcpQueryAdapter assetMcpQueryAdapter() {
			return new AssetMcpQueryAdapter(null);
		}

		@Bean
		TransactionMcpQueryAdapter transactionMcpQueryAdapter() {
			return new TransactionMcpQueryAdapter(null, null);
		}
	}
}
