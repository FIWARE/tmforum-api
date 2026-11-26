package org.fiware.tmforum.usagemanagement.domain;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.RefEntity;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = UsageSpecification.TYPE_USP)
public class UsageSpecificationRef extends RefEntity {

    public UsageSpecificationRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(UsageSpecification.TYPE_USP));
    }

}
