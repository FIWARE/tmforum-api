package org.fiware.tmforum.common.domain;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.fiware.tmforum.common.validation.ReferencedEntity;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class ReferenceValue implements ReferencedEntity {

    private URI id;
    private URI href;

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(""));
    }

    @Override
    @JsonGetter("id")
    public URI getEntityId() {
        return this.id;
    }
}
