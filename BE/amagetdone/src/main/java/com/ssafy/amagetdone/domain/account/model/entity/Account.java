package com.ssafy.amagetdone.domain.account.model.entity;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class Account extends BaseEntity {

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

	@Column(name = "account_name", length = 60)
	private String accountName;

	@Column(name = "account_type_name", length = 30)
	private String accountTypeName;

	@Column(name = "currency", length = 10)
	private String currency;

	@ColumnDefault("FALSE")
	@Column(name = "is_verified", nullable = false)
	private boolean verified;

	@ColumnDefault("TRUE")
	@Column(name = "is_active", nullable = false)
	private boolean active;

	@Column(name = "alias", length = 30)
	private String alias;

	@Column(name = "bank_code", nullable = false, length = 30)
	private String bankCode;

	@Column(name = "bank_name", nullable = false, length = 30)
	private String bankName;

	protected Account() {
	}

	public static Account create(
		User user,
		String accountNo,
		String accountTypeUniqueNo,
		String accountName,
		String accountTypeName,
		String bankCode,
		String bankName,
		String currency
	) {
		Account account = new Account();
		account.id = accountNo;
		account.user = user;
		account.accountNo = accountNo;
		account.accountTypeUniqueNo = accountTypeUniqueNo;
		account.accountName = accountName;
		account.accountTypeName = accountTypeName;
		account.bankCode = bankCode;
		account.bankName = bankName;
		account.currency = currency;
		account.verified = false;
		account.active = true;
		return account;
	}

	public void sync(
		String accountTypeUniqueNo,
		String accountName,
		String accountTypeName,
		String bankCode,
		String bankName,
		String currency
	) {
		if (hasText(accountTypeUniqueNo)) {
			this.accountTypeUniqueNo = accountTypeUniqueNo;
		}
		if (hasText(accountName)) {
			this.accountName = accountName;
		}
		if (hasText(accountTypeName)) {
			this.accountTypeName = accountTypeName;
		}
		if (hasText(bankCode)) {
			this.bankCode = bankCode;
		}
		if (hasText(bankName)) {
			this.bankName = bankName;
		}
		if (hasText(currency)) {
			this.currency = currency;
		}
	}

	public void markVerified() {
		this.verified = true;
	}

	public void markActive() {
		this.active = true;
	}

	public void markInactive() {
		this.active = false;
	}

	public String getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getAccountNo() {
		return accountNo;
	}

	public String getAccountTypeUniqueNo() {
		return accountTypeUniqueNo;
	}

	public String getAccountName() {
		return accountName;
	}

	public String getAccountTypeName() {
		return accountTypeName;
	}

	public String getCurrency() {
		return currency;
	}

	public boolean isVerified() {
		return verified;
	}

	public boolean isActive() {
		return active;
	}

	public String getAlias() {
		return alias;
	}

	public String getBankCode() {
		return bankCode;
	}

	public String getBankName() {
		return bankName;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
