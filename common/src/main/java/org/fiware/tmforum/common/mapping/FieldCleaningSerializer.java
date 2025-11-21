package org.fiware.tmforum.common.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.context.ServerRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.exception.JsonException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Slf4j
public class FieldCleaningSerializer<T> extends JsonSerializer<T> {

    public static final String FIELD_PARAMETER_SEPERATOR = ",";
    public static final List<String> MANDATORY_FIELDS = List.of("id", "href");

    // we cannot take the bean from the context, since that will be circular reference, e.g. stack-overflow
    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    {
        OBJECT_MAPPER.findAndRegisterModules();
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public void serialize(T objectToSerialize, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
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

                // If lastUpdate field exists and is not null, always include it
                if (objectNode.has("lastUpdate") && !objectNode.get("lastUpdate").isNull()) {
                    fieldsToInclude.add("lastUpdate");
                }

                Iterator<String> fieldNameIterator = jsonNode.fieldNames();

                List<String> fieldsToRemove = new ArrayList<>();

                while (fieldNameIterator.hasNext()) {
                    String fieldName = fieldNameIterator.next();
                    if (!fieldsToInclude.contains(fieldName)) {
                        fieldsToRemove.add(fieldName);
                    }
                }
                fieldsToRemove.forEach(objectNode::remove);
            }
            jsonGenerator.writeObject(objectNode);
        } else {
            throw new JsonException("Was not able to read the json node.");
        }
    }
}
