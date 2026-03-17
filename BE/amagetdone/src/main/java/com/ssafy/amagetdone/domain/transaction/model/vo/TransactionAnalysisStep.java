package com.ssafy.amagetdone.domain.transaction.model.vo;

public enum TransactionAnalysisStep {
	SYNCING,
	PREPROCESSING,
	CATEGORY_LABELING,
	CLASSIFYING_ESSENTIAL,
	BUILDING_SNAPSHOTS,
	COMPLETED
}
