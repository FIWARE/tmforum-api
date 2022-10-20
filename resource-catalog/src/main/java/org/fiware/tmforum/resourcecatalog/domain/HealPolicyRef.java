package org.fiware.tmforum.resourcecatalog.domain;

import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

// Map to HealPolicy as soon as it exists.
@MappingEnabled
public class HealPolicyRef extends RefEntity {


    public HealPolicyRef(String id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        // TODO: connect with a heal policy implementation
        return List.of("heal-policy");
    }
}
