package org.fiware.tmforum.account.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = FinancialAccount.TYPE_FINANCIALAC)
public class FinancialAccountRef extends RefEntity {

    public FinancialAccountRef(String id) {
        super(id);
    }

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "accountBalance") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "accountBalance") }))
    private AccountBalance accountBalance;

    @Override public List<String> getReferencedTypes() {
        return List.of(FinancialAccount.TYPE_FINANCIALAC);
    }
}
