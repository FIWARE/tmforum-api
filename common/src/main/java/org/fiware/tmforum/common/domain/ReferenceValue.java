package org.fiware.tmforum.common.domain;

import java.net.URI;
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
        return List.of("");
    }

    @Override
    public URI getEntityId() {
        return this.id;
    }
}
