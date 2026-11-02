package org.fiware.tmforum.customerbillmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
public class FinancialAccountRef extends RefEntity {

    public FinancialAccountRef(@JsonProperty("id") String id) {
        super(id);
    }

    // only embedded PROPERTY getters are written into a relationship (a List becomes a property list), while
    // reading a list back from the relationship needs the PROPERTY_LIST setter
    @Getter(onMethod = @__({
            @AttributeGetter(value = AttributeType.PROPERTY, targetName = "accountBalance", embedProperty = true)}))
    @Setter(onMethod = @__({
            @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "accountBalance", fromProperties = true, targetClass = AccountBalance.class)}))
    private List<AccountBalance> accountBalance;

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of("financial-account"));
    }
}
