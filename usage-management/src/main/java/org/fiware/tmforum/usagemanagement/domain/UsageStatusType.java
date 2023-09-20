package org.fiware.tmforum.usagemanagement.domain;

public enum UsageStatusType {

	RECEIVED("received"),
	REJECTED("rejected"),
	RECYCLED("recycled"),
	GUIDED("guided"),
	RATED("rated"),
	RERATED("rerated"),
	BILLED("billed");

	private final String value;

	UsageStatusType(String value) {
		this.value = value;
	}
}