package org.fiware.tmforum.servicecatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = ServiceCategory.TYPE_SERVICE_CATEGORY)
public class ServiceCategory extends EntityWithId {

    public static final String TYPE_SERVICE_CATEGORY = "service-category";

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URI href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description")}))
    private String description;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "isRoot")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "isRoot")}))
    private Boolean isRoot;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastUpdate")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastUpdate")}))
    private Instant lastUpdate;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus")}))
    private String lifecycleStatus;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "parentId")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "parentId")}))
    private ServiceCategoryRef parentId;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "version")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "version")}))
    private String version;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "category")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "category", targetClass = ServiceCategoryRef.class)}))
    private List<ServiceCategoryRef> category;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "serviceCandidate")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "serviceCandidate", targetClass = ServiceCandidateRef.class)}))
    private List<ServiceCandidateRef> serviceCandidate;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
    private TimePeriod validFor;

    public ServiceCategory(String id) {
        super(TYPE_SERVICE_CATEGORY, id);
    }
}