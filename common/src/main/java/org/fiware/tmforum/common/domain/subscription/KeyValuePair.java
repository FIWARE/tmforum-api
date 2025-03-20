package org.fiware.tmforum.common.domain.subscription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class KeyValuePair {

    private String key;
    private String value;

}
