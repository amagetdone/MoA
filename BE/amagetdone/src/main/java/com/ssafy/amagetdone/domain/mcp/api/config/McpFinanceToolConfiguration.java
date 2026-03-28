package com.ssafy.amagetdone.domain.mcp.api.config;

import com.ssafy.amagetdone.domain.mcp.application.service.AssetMcpQueryAdapter;
import com.ssafy.amagetdone.domain.mcp.application.service.BudgetMcpQueryAdapter;
import com.ssafy.amagetdone.domain.mcp.application.service.TransactionMcpQueryAdapter;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "mcp.finance", name = "enabled", havingValue = "true")
public class McpFinanceToolConfiguration {

	@Bean
	public ToolCallbackProvider financeToolCallbackProvider(
		BudgetMcpQueryAdapter budgetMcpQueryAdapter,
		AssetMcpQueryAdapter assetMcpQueryAdapter,
		TransactionMcpQueryAdapter transactionMcpQueryAdapter
	) {
		return MethodToolCallbackProvider.builder()
			.toolObjects(budgetMcpQueryAdapter, assetMcpQueryAdapter, transactionMcpQueryAdapter)
			.build();
	}
}
