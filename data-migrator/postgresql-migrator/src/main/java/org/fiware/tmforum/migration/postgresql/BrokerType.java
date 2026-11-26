package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.common.configuration.StorageBackend;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

import java.util.Arrays;
import java.util.Iterator;

/**
 * The source brokers, named like the Micronaut environment that configures each of them.
 */
public enum BrokerType {

	ORION_LD(StorageBackend.ORION_LD.environment()),
	SCORPIO(StorageBackend.SCORPIO.environment());

	private final String environment;

	BrokerType(String environment) {
		this.environment = environment;
	}

	public String getEnvironment() {
		return environment;
	}

	/**
	 * Parses the CLI value, which is the environment name.
	 */
	public static class Converter implements ITypeConverter<BrokerType> {

		@Override
		public BrokerType convert(String value) {
			return Arrays.stream(values())
					.filter(type -> type.environment.equals(value))
					.findFirst()
					.orElseThrow(() -> new TypeConversionException(
							String.format("Unknown broker type '%s', expected one of %s.", value, new Names())));
		}
	}

	/**
	 * The accepted CLI values.
	 */
	public static class Names implements Iterable<String> {

		@Override
		public Iterator<String> iterator() {
			return Arrays.stream(values()).map(BrokerType::getEnvironment).iterator();
		}

		@Override
		public String toString() {
			return String.join("|", this);
		}
	}
}
