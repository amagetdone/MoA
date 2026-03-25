package com.ssafy.amagetdone.global.init;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ssafy.amagetdone.domain.account.model.repository.CategoryRepository;
import com.ssafy.amagetdone.domain.account.application.usecase.InitializeMerchantCatalogUseCase;
import com.ssafy.amagetdone.domain.member.application.usecase.EnsureTestSocialUsersInitializedUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private EnsureTestSocialUsersInitializedUseCase ensureTestSocialUsersInitializedUseCase;

	@Mock
	private InitializeMerchantCatalogUseCase initializeMerchantCatalogUseCase;

	@Test
	void categoryInitSeedsDefaultsWhenRepositoryIsEmpty() {
		DataInitializer.InitService initService = new DataInitializer.InitService(
			categoryRepository,
			initializeMerchantCatalogUseCase,
			ensureTestSocialUsersInitializedUseCase,
			true
		);
		given(categoryRepository.count()).willReturn(0L);

		initService.categoryInit();

		verify(categoryRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void categoryInitSkipsWhenRepositoryAlreadyHasRows() {
		DataInitializer.InitService initService = new DataInitializer.InitService(
			categoryRepository,
			initializeMerchantCatalogUseCase,
			ensureTestSocialUsersInitializedUseCase,
			true
		);
		given(categoryRepository.count()).willReturn(3L);

		initService.categoryInit();

		verify(categoryRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
	}

	@Test
	void initializeSeedsCategoriesAndTestSocialUsers() {
		DataInitializer.InitService initService = new DataInitializer.InitService(
			categoryRepository,
			initializeMerchantCatalogUseCase,
			ensureTestSocialUsersInitializedUseCase,
			true
		);
		given(categoryRepository.count()).willReturn(0L);

		initService.initialize();

		verify(categoryRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
		verify(initializeMerchantCatalogUseCase).execute();
		verify(ensureTestSocialUsersInitializedUseCase).execute();
	}

	@Test
	void initializeSkipsMerchantInitWhenMerchantInitializationIsDisabled() {
		DataInitializer.InitService initService = new DataInitializer.InitService(
			categoryRepository,
			initializeMerchantCatalogUseCase,
			ensureTestSocialUsersInitializedUseCase,
			false
		);
		given(categoryRepository.count()).willReturn(0L);

		initService.initialize();

		verify(categoryRepository).saveAll(org.mockito.ArgumentMatchers.anyList());
		verify(initializeMerchantCatalogUseCase, never()).execute();
		verify(ensureTestSocialUsersInitializedUseCase).execute();
	}
}
