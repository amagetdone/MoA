package com.ssafy.amagetdone.domain.transaction.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "card_transaction_override")
@PrimaryKeyJoinColumn(name = "id")
public class CardTransactionOverride extends TransactionOverride {

	@Column(name = "transactionUniqueNo", nullable = false)
	private Long transactionUniqueNo;

	@Column(name = "merchant_id", nullable = false)
	private Long merchantId;

	@Column(name = "cardStatus", nullable = false)
	private boolean cardStatus;

	@Column(name = "billStatementsYn", nullable = false)
	private boolean billStatementsYn;

	@Column(name = "billStatementsStatus", nullable = false, length = 20)
	private String billStatementsStatus;

	@Column(name = "cardUniqueNo", nullable = false)
	private Long cardUniqueNo;

	protected CardTransactionOverride() {
	}
}
