package org.fiware.tmforum.agreement.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.List;

import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.product.CategoryRef;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;

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
        private boolean isBundle;

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
        private String lastUpdate;

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

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "relatedParty") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "relatedParty") }))
        private List<RelatedParty> relatedParty;

        @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "serviceCategory") }))
        @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "serviceCategory") }))
        private CategoryRef serviceCategory;

        @Getter(onMethod = @__({
                        @AttributeGetter(value = AttributeType.PROPERTY, targetName = "specificationCharacteristic") }))
        @Setter(onMethod = @__({
                        @AttributeSetter(value = AttributeType.PROPERTY, targetName = "specificationCharacteristic") }))
        private List<AgreementSpecCharacteristic> specificationCharacteristic;

        @Getter(onMethod = @__({
                        @AttributeGetter(value = AttributeType.PROPERTY, targetName = "specificationRelationship") }))
        @Setter(onMethod = @__({
                        @AttributeSetter(value = AttributeType.PROPERTY, targetName = "specificationRelationship") }))
        private List<AgreementSpecificationRelationship> specificationRelationship;

        @Getter(onMethod = @__({
                        @AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
        @Setter(onMethod = @__({
                        @AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
        private TimePeriod validFor;

}
