package org.fiware.tmforum.productordering.domain;

import lombok.Getter;

public enum TaskState {
	
	ACKNOWLEDGED("acknowledged"),
	TERMINATED_WITH_ERROR("terminatedWithError"),
	IN_PROGRESS("inProgress"),
	DONE("done");

	@Getter
	private final String value;

	TaskState(String value) {
		this.value = value;
	}
}
