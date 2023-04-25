package org.fiware.tmforum.agreement.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import java.util.List;
import org.fiware.tmforum.common.domain.TimePeriod;
import java.time.Instant;
import org.fiware.tmforum.product.Characteristic;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.AgreementRef;
import java.net.URI;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Agreement.TYPE_AG)
public class Agreement extends EntityWithId {
	public static final String TYPE_AG = "agreement";

	public Agreement(String id) {
		super(TYPE_AG, id);
	}

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
	private URI href;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "agreementType") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "agreementType") }))
	private String agreementType;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "description") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "description") }))
	private String description;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "documentNumber") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "documentNumber") }))
	private int documentNumber;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "initialDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "initialDate") }))
	private Instant initialDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "name") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name") }))
	private String name;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "statementOfIntent") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "statementOfIntent") }))
	private String statementOfIntent;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "status") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "status") }))
	private String status;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "version") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "version") }))
	private String version;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "agreementAuthorization") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "agreementAuthorization", targetClass = AgreementAuth.class) }))
	private List<AgreementAuth> agreementAuthorization;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "agreementItem") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "agreementItem", targetClass = AgreementItem.class) }))
	private List<AgreementItem> agreementItem;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "agreementPeriod") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "agreementPeriod") }))
	private TimePeriod agreementPeriod;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "agreementSpecification") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "agreementSpecification") }))
	private AgreementSpecificationRef agreementSpecification;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "associatedAgreement") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "associatedAgreement", targetClass = AgreementRef.class) }))
	private List<AgreementRef> associatedAgreement;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "characteristic") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "characteristic", targetClass = Characteristic.class) }))
	private List<Characteristic> characteristic;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "completionDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "completionDate") }))
	private TimePeriod completionDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "engagedParty") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "engagedParty", targetClass = RelatedParty.class, fromProperties = true) }))
	private List<RelatedParty> engagedParty;

}
