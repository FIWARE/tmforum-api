package org.fiware.tmforum.common.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RelatedPartyRefValue extends ReferenceValue {
    private String name;
    private String role;
}
