package org.fiware.tmforum.customerbillmanagement.domain;

public enum OnDemandStateValue {

	INPROGRESS("inProgress"),
	REJECTED("rejected"),
	DONE("done"),
	TERMINATEDWITHERROR("terminatedWithError");

	private final String value;

	OnDemandStateValue(String value) {
		this.value = value;
	}
}
