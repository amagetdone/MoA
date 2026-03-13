package com.ssafy.amagetdone.global.init;

import java.util.List;

import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.domain.asset.model.repository.CategoryRepository;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DataInitializer {

	private final InitService initService;

	public DataInitializer(InitService initService) {
		this.initService = initService;
	}

	@PostConstruct
	public void init() {
		initService.categoryInit();
	}

	@Component
	public static class InitService {

		private static final List<Category> DEFAULT_CATEGORIES = List.of(
			Category.create(0, "교육비"),
			Category.create(1, "교통비"),
			Category.create(2, "구독료"),
			Category.create(3, "금융/보험"),
			Category.create(4, "기타서비스"),
			Category.create(5, "문화/여가/여행"),
			Category.create(6, "미용"),
			Category.create(7, "생활/반려동물"),
			Category.create(8, "세금/공과금"),
			Category.create(9, "쇼핑/패션"),
			Category.create(10, "식비"),
			Category.create(11, "의료/건강"),
			Category.create(12, "주거/통신"),
			Category.create(13, "카페"),
			Category.create(14, "편의점")
		);

		private final CategoryRepository categoryRepository;

		public InitService(CategoryRepository categoryRepository) {
			this.categoryRepository = categoryRepository;
		}

		public void categoryInit() {
			if (categoryRepository.count() > 0) {
				categoryRepository.saveAll(DEFAULT_CATEGORIES);
			}
		}
	}
}
