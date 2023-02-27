package org.fiware.tmforum.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.Ignore;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class ServiceCandidateRef extends RefEntity {

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "version", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "version", targetClass = String.class)}))
    private String version;

    public ServiceCandidateRef(URI id) {
        super(id);
    }

    @Override
    @Ignore
    public List<String> getReferencedTypes() {
        return List.of(ServiceCandidate.TYPE_SERVICE_CANDIDATE);
    }
}
