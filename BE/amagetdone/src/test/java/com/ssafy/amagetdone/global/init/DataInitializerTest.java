package com.ssafy.amagetdone.global.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.List;

import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.domain.asset.model.repository.CategoryRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private DataInitializer.InitService initService;

	@InjectMocks
	private DataInitializer dataInitializer;

	@Test
	@SuppressWarnings("unchecked")
	void categoryInitSavesDefaultCategoriesWhenRepositoryCountIsPositive() {
		DataInitializer.InitService service = new DataInitializer.InitService(categoryRepository);
		ArgumentCaptor<List<Category>> categoriesCaptor = ArgumentCaptor.forClass(List.class);
		given(categoryRepository.count()).willReturn(1L);

		service.categoryInit();

		verify(categoryRepository).saveAll(categoriesCaptor.capture());
		List<Category> savedCategories = categoriesCaptor.getValue();
		assertEquals(15, savedCategories.size());
		assertEquals(0, savedCategories.get(0).getId());
		assertEquals("교육비", savedCategories.get(0).getName());
		assertEquals(14, savedCategories.get(14).getId());
		assertEquals("편의점", savedCategories.get(14).getName());
	}

	@Test
	void categoryInitSkipsWhenRepositoryCountIsZero() {
		DataInitializer.InitService service = new DataInitializer.InitService(categoryRepository);
		given(categoryRepository.count()).willReturn(0L);

		service.categoryInit();

		verify(categoryRepository).count();
		verify(categoryRepository, never()).saveAll(any());
	}

	@Test
	void initDelegatesToCategoryInit() {
		dataInitializer.init();

		verify(initService).categoryInit();
	}

	@Test
	void initializerAndInitServiceAreRegisteredComponents() {
		assertNotNull(DataInitializer.class.getAnnotation(Component.class));
		assertNotNull(DataInitializer.InitService.class.getAnnotation(Component.class));
	}

	@Test
	void initMethodUsesPostConstruct() throws NoSuchMethodException {
		Method initMethod = DataInitializer.class.getDeclaredMethod("init");

		assertNotNull(initMethod.getAnnotation(PostConstruct.class));
	}
}
