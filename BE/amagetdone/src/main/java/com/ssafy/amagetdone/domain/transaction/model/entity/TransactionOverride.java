package com.ssafy.amagetdone.domain.transaction.model.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction_override")
@Inheritance(strategy = InheritanceType.JOINED)
public class TransactionOverride {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	@Column(name = "transactionDateTime", nullable = false)
	private LocalDateTime transactionDateTime;

	@Column(name = "transactionBalance", nullable = false)
	private Long transactionBalance;

	@Column(name = "transactionMemo", nullable = false, columnDefinition = "TEXT")
	private String transactionMemo;

	protected TransactionOverride() {
	}
}
