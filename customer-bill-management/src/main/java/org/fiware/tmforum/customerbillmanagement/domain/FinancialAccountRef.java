package org.fiware.tmforum.customerbillmanagement.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class FinancialAccountRef extends RefEntity {

	public FinancialAccountRef(String id) {
		super(id);
	}

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "accountBalance", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "accountBalance", fromProperties = true, targetClass = AccountBalance.class) }))
	private List<AccountBalance> accountBalance;

	@Override
	public List<String> getReferencedTypes() {
		return List.of("financial-account");
	}
}
