package org.fiware.tmforum.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class Characteristic extends Entity {

	private String name;
	private String valueType;
	private Object tmfValue;
}
