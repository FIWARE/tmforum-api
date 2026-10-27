package org.fiware.tmforum.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.fiware.tmforum.common.validation.ReferencedEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReferenceValue implements ReferencedEntity {

    private URI id;
    private URI href;

    @Override
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(""));
    }

    @Override
    public URI getEntityId() {
        return this.id;
    }

    @JsonIgnore
    @Override
    public URI getReferenceHref() {
        return getHref();
    }
}
