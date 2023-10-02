package org.fiware.tmforum.partyrole.domain;

import java.util.List;

import org.fiware.tmforum.common.domain.RefEntity;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class AgreementRef extends RefEntity {
    public AgreementRef (String id){
        super(id);
    }

    @Override
	public List<String> getReferencedTypes() {
		return List.of("agreement");
	}
}
