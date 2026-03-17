package com.ssafy.amagetdone.domain.accountverification.model.entity;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.member.model.entity.User;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_verification")
public class AccountVerification extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "users_id", nullable = false)
	private User user;

	@Column(name = "account_no", nullable = false, length = 20)
	private String accountNo;

	@Column(name = "auth_text", nullable = false, length = 20)
	private String authText;

	@Column(name = "request_transaction_unique_no", nullable = false)
	private Long requestTransactionUniqueNo;

	@ColumnDefault("'REQUESTED'")
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AccountVerificationStatus status = AccountVerificationStatus.REQUESTED;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	@Column(name = "failure_reason", length = 255)
	private String failureReason;

	protected AccountVerification() {
	}

	public static AccountVerification createRequested(
		User user,
		String accountNo,
		String authText,
		Long requestTransactionUniqueNo,
		LocalDateTime requestedAt
	) {
		AccountVerification verification = new AccountVerification();
		verification.user = user;
		verification.accountNo = accountNo;
		verification.authText = authText;
		verification.requestTransactionUniqueNo = requestTransactionUniqueNo;
		verification.requestedAt = requestedAt;
		return verification;
	}

	public void markSuccess(LocalDateTime verifiedAt) {
		this.status = AccountVerificationStatus.SUCCESS;
		this.verifiedAt = verifiedAt;
		this.failureReason = null;
	}

	public void markFailed(String failureReason) {
		this.status = AccountVerificationStatus.FAILED;
		this.failureReason = failureReason;
		this.verifiedAt = null;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getAccountNo() {
		return accountNo;
	}

	public String getAuthText() {
		return authText;
	}

	public Long getRequestTransactionUniqueNo() {
		return requestTransactionUniqueNo;
	}

	public AccountVerificationStatus getStatus() {
		return status;
	}

	public LocalDateTime getRequestedAt() {
		return requestedAt;
	}

	public LocalDateTime getVerifiedAt() {
		return verifiedAt;
	}

	public String getFailureReason() {
		return failureReason;
	}
}
