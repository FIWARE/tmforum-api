package org.fiware.tmforum.common.mapping;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.context.ServerRequestContext;
import io.micronaut.runtime.http.scope.RequestScope;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.PropertyDefinition;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.util.*;

@Singleton
@RequiredArgsConstructor
public class EntityExtender {

    private final ObjectMapper objectMapper;
    private final Map<Integer, Map<Integer, Map>> voHashs = new HashMap<>();

    public <C extends Entity> C handleExtension(Object sourceVO, C entity) {
        if (entity.getAtSchemaLocation() == null) {
            return entity;
        }
        if (entity.getAtSchemaLocation() != null) {
            HttpClient httpClient = new DefaultHttpClient();
            HttpRequest<String> req = HttpRequest.GET(entity.getAtSchemaLocation());
            String schemaString = Mono.from(httpClient.retrieve(req)).block();
            ObjectMapper om = new ObjectMapper();
            try {
                Map m = om.readValue(schemaString, Map.class);
                Map<String, PropertyDefinition> msp = new LinkedHashMap<>();
                if (m.containsKey("properties")) {
                    msp = om.convertValue(m.get("properties"), new TypeReference<Map<String, PropertyDefinition>>() {
                    });
                } else if (m.containsKey("definitions")) {
                    msp = om.convertValue(m.get("definitions"), new TypeReference<Map<String, PropertyDefinition>>() {
                    });
                }
                List<String> requiredProps = new ArrayList<>();
                if (m.containsKey("required")) {
                    requiredProps = om.convertValue(m.get("required"), new TypeReference<List<String>>() {
                    });
                }
                if (!msp.isEmpty()) {
                    Integer requestId = ServerRequestContext.currentRequest().hashCode();
                    Map<Integer, Map> extensionContext = voHashs.getOrDefault(requestId, Map.of());
                    Map voExtensionContext = extensionContext.getOrDefault(sourceVO.hashCode(), Map.of());
                    if (requiredProps.stream().anyMatch(rp -> !voExtensionContext.containsKey(rp))) {
                        throw new IllegalArgumentException("Required properties not contained.");
                    }
                    msp.forEach((key, value) -> {
                        if (voExtensionContext.containsKey(key)) {
                            entity.addAdditionalProperties(key, voExtensionContext.get(key));
                        }
                    });
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return entity;
    }

    public Object addAndDeserializeVO(Integer requestId, Map plain, Class<?> targetClass) {
        Object targetVO = objectMapper.convertValue(plain, targetClass);
        if (voHashs.containsKey(requestId)) {
            voHashs.get(requestId).put(targetVO.hashCode(), plain);
        } else {
            Map<Integer, Map> voMap = new LinkedHashMap<>();
            voMap.put(targetVO.hashCode(), plain);
            voHashs.put(requestId, voMap);
        }
        return targetVO;
    }

    public boolean containsRequest(int id) {
        return voHashs.containsKey(id);
    }

    public void storeExtensions(Entity e) {
//        Map<String, Object> entityExtensions = new LinkedHashMap<>();
//        e.getAdditionalProperties().forEach(ap -> {
//            entityExtensions.put(ap.getName(), ap.getValue());
//        });
    }
}
