package org.fiware.tmforum.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract super class for all entities to be created
 */
@EqualsAndHashCode
@RequiredArgsConstructor
public abstract class Entity {

    /**
     * When sub-classing, this defines the super-class
     */
    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "atBaseType", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY, targetName = "atBaseType", fromProperties = true)}))
    String atBaseType;

    /**
     * A URI to a JSON-Schema file that defines additional attributes and relationships
     */
    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "atSchemaLocation", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY, targetName = "atSchemaLocation", fromProperties = true, targetClass = URI.class)}))
    URI atSchemaLocation;

    /**
     * When sub-classing, this defines the sub-class entity name
     */
    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "atType", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY, targetName = "atType", fromProperties = true)}))
    String atType;

    @JsonIgnore
    public String getEntityState() {
        return "default";
    }

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "additionalProperties") }))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "additionalProperties", targetClass = AdditionalProperty.class) }))
    private List<AdditionalProperty> additionalProperties;

    public void addAdditionalProperties(String propertyKey, Object value) {
        AdditionalProperty ap = new AdditionalProperty(propertyKey, value);
        if (this.additionalProperties == null) {
            this.additionalProperties = new ArrayList<>();
        }
        this.additionalProperties.add(ap);
    }

}
