package org.fiware.tmforum.party.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.util.List;

@MappingEnabled(entityType = TaxExemptionCertificate.TYPE_TAX_EXEMPTION_CERTIFICATE)
@EqualsAndHashCode(callSuper = true)
public class TaxExemptionCertificate extends EntityWithId {

	public static final String TYPE_TAX_EXEMPTION_CERTIFICATE = "tax-exemption-certificate";

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "attachment")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "attachment")}))
	private AttachmentRefOrValue attachment;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "taxDefinition")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "taxDefinition", targetClass = TaxDefinition.class)}))
	private List<TaxDefinition> taxDefinition;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
	private TimePeriod validFor;

	public TaxExemptionCertificate(String id) {
		super(TYPE_TAX_EXEMPTION_CERTIFICATE, id);
	}
}
