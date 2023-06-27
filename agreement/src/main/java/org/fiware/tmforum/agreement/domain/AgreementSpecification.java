package org.fiware.tmforum.agreement.domain;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.product.CategoryRef;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = AgreementSpecification.TYPE_AGSP)
public class AgreementSpecification extends EntityWithId {
        public static final String TYPE_AGSP = "agreementSpecification";

        public AgreementSpecification(String id) {
                super(AgreementSpecification.TYPE_AGSP, id);
        }

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
        private URI href;

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "description") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "description") }))
        private String description;

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "isBundle") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "isBundle") }))
        private Boolean isBundle;

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
        private Instant lastUpdate;

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus") }))
        private String lifecycleStatus;

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "name") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name") }))
        private String name;

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "version") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "version") }))
        private String version;

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "attachment") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "attachment") }))
        private List<AttachmentRefOrValue> attachment;

        @Getter(onMethod = @__({
                        @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty") }))
        @Setter(onMethod = @__({
                        @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty") }))
        private List<RelatedParty> relatedParty;

        @Getter(onMethod = @__({
                        @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "serviceCategory") }))
        @Setter(onMethod = @__({
                        @AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "serviceCategory", targetClass = CategoryRef.class, fromProperties = true) }))
        private CategoryRef serviceCategory;

        @Getter(onMethod = @__({
                        @AttributeGetter(value = AttributeType.PROPERTY, targetName = "specificationCharacteristic") }))
        @Setter(onMethod = @__({
                        @AttributeSetter(value = AttributeType.PROPERTY, targetName = "specificationCharacteristic") }))
        private List<AgreementSpecCharacteristic> specificationCharacteristic;

        @Getter(onMethod = @__({
                        @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "specificationRelationship") }))
        @Setter(onMethod = @__({
                        @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "specificationRelationship") }))
        private List<AgreementSpecificationRelationship> specificationRelationship;

        @Getter(onMethod = @__({
                        @AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
        @Setter(onMethod = @__({
                        @AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
        private TimePeriod validFor;

}
