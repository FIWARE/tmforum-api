package org.fiware.tmforum.resourcefunction.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

// Map to HealPolicy as soon as it exists.
@MappingEnabled(entityType = "heal-policy")
public class HealPolicyRef extends RefEntity {


    public HealPolicyRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of("heal-policy"));
    }
}
