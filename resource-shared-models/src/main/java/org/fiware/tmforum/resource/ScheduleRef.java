package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.List;

// TODO: map to schedule
@EqualsAndHashCode(callSuper = true)
@MappingEnabled
public class ScheduleRef extends RefEntity {

    public ScheduleRef(String id) {
        super(id);
    }

    @Override
    public List<String> getReferencedTypes() {
        return null;
    }
}
