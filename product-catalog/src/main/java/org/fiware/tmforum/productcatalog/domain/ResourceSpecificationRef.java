package org.fiware.tmforum.productcatalog.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.resource.ApiSpecification;
import org.fiware.tmforum.resource.HostingPlatformRequirementSpecification;
import org.fiware.tmforum.resource.LogicalResourceSpecification;
import org.fiware.tmforum.resource.PhysicalResourceSpecification;
import org.fiware.tmforum.resource.ResourceSpecification;
import org.fiware.tmforum.resource.SoftwareResourceSpecification;
import org.fiware.tmforum.resource.SoftwareSpecification;
import org.fiware.tmforum.resource.SoftwareSupportPackageSpecification;

import java.util.ArrayList;
import java.util.List;

@MappingEnabled(entityType = ResourceSpecification.TYPE_RESOURCE_SPECIFICATION)
@EqualsAndHashCode(callSuper = true)
public class ResourceSpecificationRef extends RefEntity {

    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "version", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY, targetName = "version", fromProperties = true)}))
    private String version;

    public ResourceSpecificationRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(
                ResourceSpecification.TYPE_RESOURCE_SPECIFICATION,
                PhysicalResourceSpecification.TYPE_PHYSICAL_RESOURCE_SPECIFICATION,
                LogicalResourceSpecification.TYPE_LOGICAL_RESOURCE_SPECIFICATION,
                SoftwareResourceSpecification.TYPE_SOFTWARE_RESOURCE_SPECIFICATION,
                SoftwareSupportPackageSpecification.TYPE_SOFTWARE_SUPPORT_PACKAGE_SPECIFICATION,
                ApiSpecification.TYPE_API_SPECIFICATION,
                SoftwareSpecification.TYPE_SOFTWARE_SPECIFICATION,
                HostingPlatformRequirementSpecification.TYPE_HOSTING_PLATFORM_REQUIREMENT_SPECIFICATION
        ));
    }
}
