package org.fiware.tmforum.common.configuration;

import org.fiware.tmforum.common.CommonConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The storage backends, each selected by the Micronaut environment of the same name.
 */
public enum StorageBackend {

	POSTGRESQL(CommonConstants.POSTGRESQL_ENVIRONMENT, false),
	ORION_LD(CommonConstants.ORION_LD_ENVIRONMENT, true),
	SCORPIO(CommonConstants.SCORPIO_ENVIRONMENT, true);

	private final String environment;
	private final boolean broker;

	StorageBackend(String environment, boolean broker) {
		this.environment = environment;
		this.broker = broker;
	}

	public String environment() {
		return environment;
	}

	public boolean isBroker() {
		return broker;
	}

	/**
	 * The backends among the given environment names; empty if none is named.
	 */
	public static Set<StorageBackend> fromEnvironments(Collection<String> environments) {
		return Arrays.stream(values())
				.filter(backend -> environments.contains(backend.environment))
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(StorageBackend.class)));
	}
}
