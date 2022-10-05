package org.fiware.tmforum.productcatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URL;

@MappingEnabled(entityType = BundledProductOfferingPriceRelationship.TYPE_BUNDLE_POP_RELATIONSHIP)
@EqualsAndHashCode(callSuper = true)
public class BundledProductOfferingPriceRelationship extends EntityWithId {

	public static final String TYPE_BUNDLE_POP_RELATIONSHIP = "bundle-pop-relationship";

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
	private URL href;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
	private String name;

	public BundledProductOfferingPriceRelationship(String id) {
		super(TYPE_BUNDLE_POP_RELATIONSHIP, id);
	}
}
