package org.fiware.tmforum.account.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = FinancialAccount.TYPE_FINANCIALAC)
public class FinancialAccountRef extends RefEntity {

    public FinancialAccountRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "accountBalance")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "accountBalance")}))
    private AccountBalance accountBalance;

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(FinancialAccount.TYPE_FINANCIALAC));
    }
}
