package com.ssafy.amagetdone.domain.asset.model.entity;

import java.time.LocalDateTime;

import com.ssafy.amagetdone.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
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
@Table(name = "card")
public class Card extends BaseEntity {

	@Id
	@Column(name = "cardUniqueNo", nullable = false)
	private Long cardUniqueNo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "withdrawalAccountNo", referencedColumnName = "id", nullable = false)
	private Account withdrawalAccount;

	@Column(name = "cvc", nullable = false, length = 3)
	private String cvc;

	@Column(name = "cardNo", nullable = false, length = 20)
	private String cardNo;

	@Column(name = "name", nullable = false, length = 20)
	private String name;

	@Column(name = "issuerCode", nullable = false, length = 10)
	private String issuerCode;

	@Column(name = "cardIssuerName", nullable = false, length = 20)
	private String cardIssuerName;

	@Column(name = "cardExpiryDate", nullable = false)
	private LocalDateTime cardExpiryDate;

	@Column(name = "cardDescription", nullable = false, length = 255)
	private String cardDescription;

	@Column(name = "withdrawalDate", nullable = false, length = 10)
	private String withdrawalDate;

	@Column(name = "baselinePerformance", nullable = false)
	private Long baselinePerformance;

	@Column(name = "maxBenefitLimit", nullable = false)
	private Long maxBenefitLimit;
}
