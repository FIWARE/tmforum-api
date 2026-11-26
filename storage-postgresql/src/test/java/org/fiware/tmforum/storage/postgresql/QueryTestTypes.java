package org.fiware.tmforum.storage.postgresql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Query classes whose bean names and document keys differ in the ways the domain classes do.
 */
final class QueryTestTypes {

    private QueryTestTypes() {
    }

    @Getter
    @Setter
    public static class PathRoot {

        private String name;
        private Number count;
        private Boolean active;
        private Instant lastUpdate;
        private List<String> tags;
        private List<PathChild> children;
        private PathChild[] childArray;

        // NGSI-LD names it otherSub, the document keeps the bean name
        @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "otherSub")}))
        @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "otherSub")}))
        private PathChild otherNamedSub;

        @JsonProperty("renamedKey")
        private String jsonRenamed;
    }

    // the shape of AppliedBillingRateCharacteristic: "value" is a bridge to the stored "tmfValue"
    @Getter
    @Setter
    public static class PathChild {

        private String name;
        private Object tmfValue;

        @JsonIgnore
        @AttributeGetter(value = AttributeType.PROPERTY, targetName = "tmfValue")
        public Object getValue() {
            return tmfValue;
        }

        @JsonIgnore
        @AttributeSetter(value = AttributeType.PROPERTY, targetName = "tmfValue", targetClass = Object.class)
        public void setValue(Object value) {
            this.tmfValue = value;
        }
    }
}
