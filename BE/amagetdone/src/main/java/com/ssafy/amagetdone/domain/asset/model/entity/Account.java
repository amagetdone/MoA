package com.ssafy.amagetdone.domain.asset.model.entity;

import com.ssafy.amagetdone.domain.member.model.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class Account {

	@Id
	@Column(name = "id", nullable = false, length = 30)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@Column(name = "account_no", nullable = false, length = 20)
	private String accountNo;

	@Column(name = "account_type_unique_no", length = 30)
	private String accountTypeUniqueNo;

	@Column(name = "is_verified", nullable = false)
	private boolean verified;

	@Column(name = "alias", length = 30)
	private String alias;

	@Column(name = "bank_code", nullable = false, length = 30)
	private String bankCode;

	@Column(name = "bank_name", nullable = false, length = 30)
	private String bankName;

	protected Account() {
	}
}
