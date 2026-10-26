package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.JsonException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Slf4j
public class FieldCleaningSerializer<T> extends ValueSerializer<T> {

    public static final String FIELD_PARAMETER_SEPERATOR = ",";
    public static final List<String> MANDATORY_FIELDS = List.of("id", "href");

    // we cannot take the bean from the context, since that will be circular reference, e.g. stack-overflow
    private final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @Override
    public void serialize(T objectToSerialize, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
        Optional<String> optionalFieldsParameter = ServerRequestContext
                .currentRequest()
                .map(HttpRequest::getParameters)
                .flatMap(params -> Optional.ofNullable(params.get("fields")));

        JsonNode jsonNode = OBJECT_MAPPER.valueToTree(objectToSerialize);
        if (jsonNode instanceof ObjectNode objectNode) {
            if (optionalFieldsParameter.isPresent()) {
                List<String> fieldsToInclude = new ArrayList<>();
                fieldsToInclude.addAll(Arrays.asList(optionalFieldsParameter.get().split(FIELD_PARAMETER_SEPERATOR)));
                fieldsToInclude.addAll(MANDATORY_FIELDS);

                Iterator<String> fieldNameIterator = jsonNode.propertyNames().iterator();

                List<String> fieldsToRemove = new ArrayList<>();

                while (fieldNameIterator.hasNext()) {
                    String fieldName = fieldNameIterator.next();
                    if (!fieldsToInclude.contains(fieldName)) {
                        fieldsToRemove.add(fieldName);
                    }
                }
                fieldsToRemove.forEach(objectNode::remove);
            }
            jsonGenerator.writePOJO(objectNode);
        } else {
            throw new JsonException("Was not able to read the json node.");
        }
    }
}
