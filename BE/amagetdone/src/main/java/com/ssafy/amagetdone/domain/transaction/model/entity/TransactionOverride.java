package com.ssafy.amagetdone.domain.transaction.model.entity;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.domain.asset.model.entity.Category;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "transaction_override")
@Inheritance(strategy = InheritanceType.JOINED)
public class TransactionOverride extends BaseEntity {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(name = "transactionDateTime", nullable = false)
	private LocalDateTime transactionDateTime;

	@Column(name = "transactionBalance", nullable = false)
	private Long transactionBalance;

	@Column(name = "transactionMemo", nullable = false, columnDefinition = "TEXT")
	private String transactionMemo;
}
