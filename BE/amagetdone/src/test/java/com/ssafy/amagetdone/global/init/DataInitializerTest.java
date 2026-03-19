package com.ssafy.amagetdone.global.init;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Test
	void categoryInitSeedsDefaultsWhenRepositoryIsEmpty() {
		DataInitializer.InitService initService = new DataInitializer.InitService(categoryRepository);
		given(categoryRepository.count()).willReturn(0L);

		initService.categoryInit();

		verify(categoryRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void categoryInitSkipsWhenRepositoryAlreadyHasRows() {
		DataInitializer.InitService initService = new DataInitializer.InitService(categoryRepository);
		given(categoryRepository.count()).willReturn(3L);

		initService.categoryInit();

		verify(categoryRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
	}
}
