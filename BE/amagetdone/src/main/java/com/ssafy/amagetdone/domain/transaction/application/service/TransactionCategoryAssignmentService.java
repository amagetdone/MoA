package com.ssafy.amagetdone.domain.transaction.application.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ssafy.amagetdone.domain.account.model.entity.Category;
import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.transaction.infra.client.MerchantInferenceApiClient;
import com.ssafy.amagetdone.domain.transaction.infra.client.dto.MerchantInferenceDto;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionCategoryAssignmentService {

	private static final int DEFAULT_CATEGORY_ID = 4;

	private final CategoryRepository categoryRepository;
	private final MerchantInferenceApiClient merchantInferenceApiClient;

	public Map<String, Category> assignCategories(List<String> sourceTexts) {
		Map<String, Category> assignedCategories = new LinkedHashMap<>();
		if (sourceTexts == null || sourceTexts.isEmpty()) {
			return assignedCategories;
		}

		Map<String, Category> categoriesByName = categoryRepository.findAll().stream()
			.collect(Collectors.toMap(Category::getName, Function.identity(), (left, right) -> left, LinkedHashMap::new));
		Category defaultCategory = Optional.ofNullable(categoriesByName.get("Other"))
			.orElseGet(() -> categoriesByName.values().stream()
				.filter(category -> category.getId() == DEFAULT_CATEGORY_ID)
				.findFirst()
				.orElseGet(() -> categoryRepository.getReferenceById(DEFAULT_CATEGORY_ID)));
		List<String> normalizedTexts = sourceTexts.stream()
			.map(this::normalizeText)
			.filter(this::hasText)
			.distinct()
			.toList();

		Map<String, MerchantInferenceDto.Result> labelsByText = normalizedTexts.isEmpty()
			? Map.of()
			: merchantInferenceApiClient.labelMerchants(normalizedTexts).results().stream()
				.collect(Collectors.toMap(
					result -> normalizeText(hasText(result.merchantText()) ? result.merchantText() : result.normalizedMerchantText()),
					Function.identity(),
					(left, right) -> left,
					LinkedHashMap::new
				));

		for (String sourceText : sourceTexts) {
			String normalizedText = normalizeText(sourceText);
			MerchantInferenceDto.Result result = labelsByText.get(normalizedText);
			Category resolvedCategory = result == null
				? defaultCategory
				: categoriesByName.getOrDefault(result.finalLabel(), defaultCategory);
			assignedCategories.put(sourceText, resolvedCategory);
		}

		return assignedCategories;
	}

	private String normalizeText(String sourceText) {
		if (!hasText(sourceText)) {
			return "";
		}
		return sourceText.trim().replaceAll("\\s+", " ");
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
