package org.fiware.tmforum.resource;

import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.PlaceRef;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;

public class PlaceRefInRole extends PlaceRef {

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "role", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "role", fromProperties = true)}))
    private String role;

    public PlaceRefInRole(String id) {
        super(id);
    }

}
