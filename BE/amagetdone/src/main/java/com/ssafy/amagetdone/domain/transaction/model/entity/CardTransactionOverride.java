package com.ssafy.amagetdone.domain.transaction.model.entity;

import com.ssafy.amagetdone.domain.asset.model.entity.Card;
import com.ssafy.amagetdone.domain.asset.model.entity.Merchant;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "card_transaction_override")
@PrimaryKeyJoinColumn(name = "id")
public class CardTransactionOverride extends TransactionOverride {

	@Column(name = "transactionUniqueNo", nullable = false)
	private Long transactionUniqueNo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "merchant_id", nullable = false)
	private Merchant merchant;

	@Column(name = "cardStatus", nullable = false)
	private boolean cardStatus;

	@Column(name = "billStatementsYn", nullable = false)
	private boolean billStatementsYn;

	@ColumnDefault("'미결제'")
	@Column(name = "billStatementsStatus", nullable = false, length = 20)
	private String billStatementsStatus = "미결제";

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cardUniqueNo", referencedColumnName = "cardUniqueNo", nullable = false)
	private Card card;

	protected CardTransactionOverride() {
	}
}
