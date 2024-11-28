package org.fiware.tmforum.resourcefunction.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

public class ResourceGraphRef extends RefEntity {

    public ResourceGraphRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(getAtReferredType()));
    }
}
