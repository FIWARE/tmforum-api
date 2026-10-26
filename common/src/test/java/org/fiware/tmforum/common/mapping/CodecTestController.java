package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.ngsi.model.PropertyListVO;
import org.fiware.ngsi.model.PropertyVO;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Test-only endpoints that push VOs through the HTTP JSON codec, so {@link MapperCodecRegressionTest} sees the
 * context mapper exactly as the API controllers do.
 */
@Requires(property = StubBrokerController.SPEC_NAME_PROPERTY, value = MapperCodecRegressionTest.SPEC_NAME)
@Controller(CodecTestController.BASE_PATH)
public class CodecTestController {

	public static final String BASE_PATH = "/codec-test";
	public static final String VALIDATED_PATH = "/validated";
	public static final String FIELD_CLEANED_PATH = "/field-cleaned";
	public static final String PLAIN_PATH = "/plain";

	public static final String ID = "urn:codec-test:1";
	public static final String HREF = "/codec-test/1";
	public static final String NAME = "name";
	public static final String DESCRIPTION = "description";
	public static final String PROPERTY_VALUE = "property-value";

	@Post(VALIDATED_PATH)
	public ValidatedVO echo(@Body ValidatedVO validatedVO) {
		return validatedVO;
	}

	@Get(FIELD_CLEANED_PATH)
	public FieldCleanedVO fieldCleaned() {
		FieldCleanedVO fieldCleanedVO = new FieldCleanedVO();
		fieldCleanedVO.setId(ID);
		fieldCleanedVO.setHref(HREF);
		fieldCleanedVO.setName(NAME);
		fieldCleanedVO.setDescription(DESCRIPTION);
		return fieldCleanedVO;
	}

	@Get(PLAIN_PATH)
	public PlainVO plain() {
		PropertyListVO propertyList = new PropertyListVO();
		propertyList.add(new PropertyVO().value(PROPERTY_VALUE));
		PlainVO plainVO = new PlainVO();
		plainVO.setId(ID);
		plainVO.setTags(new ArrayList<>());
		plainVO.setProperties(propertyList);
		return plainVO;
	}

	/**
	 * Mirrors a generated VO that supports {@code @schemaLocation} extensions.
	 */
	@Data
	@EqualsAndHashCode(callSuper = true)
	public static class ValidatedVO extends UnknownPreservingBase {
		@JsonProperty("id")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String id;
		@JsonProperty("@schemaLocation")
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private URI atSchemaLocation;
	}

	/**
	 * VO registered with the {@link FieldCleaningSerializer} through {@link CodecTestFieldCleaningModule}.
	 */
	@Data
	public static class FieldCleanedVO {
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String id;
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String href;
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String name;
		@JsonInclude(JsonInclude.Include.NON_NULL)
		private String description;
	}

	/**
	 * VO without per-field inclusion, so the configured {@code jackson.serialization-inclusion} applies.
	 */
	@Data
	public static class PlainVO {
		private String id;
		private String unset;
		private List<String> tags;
		private PropertyListVO properties;
	}
}
