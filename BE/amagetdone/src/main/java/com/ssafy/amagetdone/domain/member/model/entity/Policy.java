package com.ssafy.amagetdone.domain.member.model.entity;

import java.util.ArrayList;
import java.util.List;

import com.ssafy.amagetdone.domain.member.model.vo.AgreementType;
import com.ssafy.amagetdone.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Table(name = "policy")
public class Policy extends BaseEntity {

	@Id
	@Column(name = "id", nullable = false)
	private Integer id;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "agreement_type", nullable = false)
	private AgreementType agreementType;

	@Column(name = "required", nullable = false)
	private boolean required;

	@Column(name = "version", nullable = false)
	private Integer version;

	@Builder.Default
	@OneToMany(mappedBy = "policy")
	private List<Agreement> agreements = new ArrayList<>();
}
