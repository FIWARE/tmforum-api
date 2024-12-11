package org.fiware.tmforum.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyDefinition {

    private String type;
    private String format;
}
