package com.ssafy.amagetdone.domain.transaction.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.transaction.infra.client.MerchantInferenceApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.MerchantInferenceDto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionCategoryAssignmentServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private MerchantInferenceApiClient merchantInferenceApiClient;

	@Test
	void assignCategoriesMapsAiLabelsToExistingCategoriesAndFallsBackToDefault() {
		TransactionCategoryAssignmentService service = new TransactionCategoryAssignmentService(
			categoryRepository,
			merchantInferenceApiClient
		);
		Category foodCategory = Category.create(1, "Food");
		Category defaultCategory = Category.create(4, "Other");
		given(categoryRepository.findAll()).willReturn(List.of(foodCategory, defaultCategory));
		given(merchantInferenceApiClient.labelMerchants(List.of("Starbucks", "Unknown"))).willReturn(
			new MerchantInferenceDto.Response(List.of(
				new MerchantInferenceDto.Result("Starbucks", "Starbucks", "Food", "Food", 0.98, false, 0.7),
				new MerchantInferenceDto.Result("Unknown", "Unknown", "Missing", "Missing", 0.55, true, 0.7)
			))
		);

		Map<String, Category> assignedCategories = service.assignCategories(List.of(" Starbucks ", "Unknown"));

		assertThat(assignedCategories.get(" Starbucks ")).isSameAs(foodCategory);
		assertThat(assignedCategories.get("Unknown")).isSameAs(defaultCategory);
		verify(merchantInferenceApiClient).labelMerchants(List.of("Starbucks", "Unknown"));
	}

	@Test
	void assignCategoriesUsesMerchantTextFromResponseForLookup() {
		TransactionCategoryAssignmentService service = new TransactionCategoryAssignmentService(
			categoryRepository,
			merchantInferenceApiClient
		);
		Category transportCategory = Category.create(1, "교통비");
		Category defaultCategory = Category.create(4, "기타서비스");
		given(categoryRepository.findAll()).willReturn(List.of(transportCategory, defaultCategory));
		given(merchantInferenceApiClient.labelMerchants(List.of("UT 택시"))).willReturn(
			new MerchantInferenceDto.Response(List.of(
				new MerchantInferenceDto.Result("UT 택시", "ut 택시", "교통비", "교통비", 0.96, false, 0.7)
			))
		);

		Map<String, Category> assignedCategories = service.assignCategories(List.of("UT 택시"));

		assertThat(assignedCategories.get("UT 택시")).isSameAs(transportCategory);
		verify(merchantInferenceApiClient).labelMerchants(List.of("UT 택시"));
	}
}
