package org.fiware.tmforum.common.domain.subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class KeyValuePair {

    // key is not allowed as plain entry for mapping
    private String pairKey;
    private String value;

}
