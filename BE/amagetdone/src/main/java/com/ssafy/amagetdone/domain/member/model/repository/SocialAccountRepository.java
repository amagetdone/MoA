package com.ssafy.amagetdone.domain.member.model.repository;

import java.util.Optional;

import com.ssafy.amagetdone.domain.member.model.entity.SocialAccount;
import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Integer> {
	Optional<SocialAccount> findByProviderAndProviderUid(SocialProvider provider, String providerUid);
}
