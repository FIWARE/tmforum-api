package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public enum JobStateTypeVO {

	NOT_STARTED("Not Started"),
	RUNNING("Running"),
	SUCCEEDED("Succeeded"),
	FAILED("Failed");

	public static final java.lang.String NOT_STARTED_VALUE = "Not Started";
	public static final java.lang.String RUNNING_VALUE = "Running";
	public static final java.lang.String SUCCEEDED_VALUE = "Succeeded";
	public static final java.lang.String FAILED_VALUE = "Failed";

	private final java.lang.String value;

	private JobStateTypeVO(java.lang.String value) {
		this.value = value;
	}

	@com.fasterxml.jackson.annotation.JsonCreator
	public static JobStateTypeVO toEnum(java.lang.String value) {
		return toOptional(value).orElseThrow(() -> new IllegalArgumentException("Unknown value '" + value + "'."));
	}

	public static java.util.Optional<JobStateTypeVO> toOptional(java.lang.String value) {
		return java.util.Arrays
				.stream(values())
				.filter(e -> e.value.equals(value))
				.findAny();
	}

	@com.fasterxml.jackson.annotation.JsonValue
	public java.lang.String getValue() {
		return value;
	}
}
