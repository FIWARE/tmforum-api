package org.fiware.tmforum.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference to a ResourceSpecification entity that can be any of the sub-types:
 * resource-specification, physical-resource-specification, logical-resource-specification,
 * software-resource-specification, software-support-package-specification, api-specification,
 * software-specification, hosting-platform-requirement-specification.
 * The @MappingEnabled annotation registers all allowed types for NGSI-LD entity matching.
 */
@MappingEnabled(entityType = {
        "resource-specification",
        "physical-resource-specification",
        "logical-resource-specification",
        "software-resource-specification",
        "software-support-package-specification",
        "api-specification",
        "software-specification",
        "hosting-platform-requirement-specification"
})
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
