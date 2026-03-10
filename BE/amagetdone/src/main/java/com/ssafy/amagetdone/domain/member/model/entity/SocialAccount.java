package com.ssafy.amagetdone.domain.member.model.entity;

import com.ssafy.amagetdone.domain.member.model.vo.SocialProvider;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "social_account",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = {"provider", "provider_uid"})
	}
)
public class SocialAccount extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Integer id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false, unique = true)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 20)
	private SocialProvider provider;

	@Column(name = "provider_uid", nullable = false, length = 255)
	private String providerUid;

	protected SocialAccount() {
	}

	public static SocialAccount create(User user, SocialProvider provider, String providerUid) {
		SocialAccount socialAccount = new SocialAccount();
		socialAccount.user = user;
		socialAccount.provider = provider;
		socialAccount.providerUid = providerUid;
		user.linkSocialAccount(socialAccount);
		return socialAccount;
	}

	public User getUser() {
		return user;
	}

	public SocialProvider getProvider() {
		return provider;
	}

	public String getProviderUid() {
		return providerUid;
	}
}
