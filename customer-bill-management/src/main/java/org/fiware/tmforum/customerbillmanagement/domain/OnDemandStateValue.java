package org.fiware.tmforum.customerbillmanagement.domain;

import lombok.Getter;

public enum OnDemandStateValue {

	INPROGRESS("inProgress"),
	REJECTED("rejected"),
	DONE("done"),
	TERMINATEDWITHERROR("terminatedWithError");

	@Getter
	private final String value;

	OnDemandStateValue(String value) {
		this.value = value;
	}
}
