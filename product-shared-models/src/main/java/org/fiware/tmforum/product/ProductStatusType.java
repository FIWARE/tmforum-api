package org.fiware.tmforum.product;

public enum ProductStatusType {

	CREATED("created"),
	PENDINGACTIVE("pendingActive"),
	CANCELLED("cancelled"),
	ACTIVE("active"),
	PENDINGTERMINATE("pendingTerminate"),
	TERMINATED("terminated"),
	SUSPENDED("suspended"),
	ABORTED_("aborted ");

	private final String value;

	ProductStatusType(String value) {
		this.value = value;
	}
}
