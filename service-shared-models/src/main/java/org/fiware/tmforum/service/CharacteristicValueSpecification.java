package org.fiware.tmforum.service;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Data
public class CharacteristicValueSpecification {

    private Boolean isDefault;
    private String rangeInterval;
    private String regex;
    private String unitOfMeasure;
    private Integer valueFrom;
    private Integer valueTo;
    private String valueType;
    private TimePeriod validFor;
    private Object tmfValue;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String propertyKey, Object value) {
        if (this.additionalProperties == null) {
            this.additionalProperties = new HashMap<>();
        }
        this.additionalProperties.put(propertyKey, value);
    }

    @JsonIgnore
    public void addAdditionalProperty(String propertyKey, Object value) {
        if (this.additionalProperties == null) {
            this.additionalProperties = new HashMap<>();
        }
        this.additionalProperties.put(propertyKey, value);
    }
}
