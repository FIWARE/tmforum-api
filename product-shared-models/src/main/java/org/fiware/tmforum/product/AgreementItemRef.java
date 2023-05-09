package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class AgreementItemRef extends RefEntity {

	public AgreementItemRef(String id) {
		super(id);
	}

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "agreementItemId", embedProperty = true) }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "agreementItemId", fromProperties = true) }))
	private String agreementItemId;

	@Override
	public List<String> getReferencedTypes() {
		return List.of("agreement");
	}
}
