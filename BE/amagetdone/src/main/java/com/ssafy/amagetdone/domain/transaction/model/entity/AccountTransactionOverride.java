package com.ssafy.amagetdone.domain.transaction.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@Column(name = "account_id", nullable = false)
	private Integer accountId;

	protected AccountTransactionOverride() {
	}
}
