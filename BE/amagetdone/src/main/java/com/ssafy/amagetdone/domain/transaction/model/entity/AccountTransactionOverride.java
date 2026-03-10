package com.ssafy.amagetdone.domain.transaction.model.entity;

import com.ssafy.amagetdone.domain.asset.model.entity.Account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_transaction_override")
@PrimaryKeyJoinColumn(name = "id")
public class AccountTransactionOverride extends TransactionOverride {

	@Column(name = "transactionUniqueNo", nullable = false)
	private Long transactionUniqueNo;

	@Column(name = "transactionType", nullable = false)
	private Integer transactionType;

	@Column(name = "transactionTypeName", nullable = false, length = 20)
	private String transactionTypeName;

	@Column(name = "transactionAfterBalance", nullable = false)
	private Long transactionAfterBalance;

	@Column(name = "transactionSummary", nullable = false, length = 30)
	private String transactionSummary;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
	private Account account;

	protected AccountTransactionOverride() {
	}
}
