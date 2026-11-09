package org.fiware.tmforum.storage.postgresql;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.fiware.tmforum.common.domain.Characteristic;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.Money;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.storage.NotStored;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Domain-like classes shaped like the real ones: id-only constructors, final type, lists, nested values, references.
 */
final class DocumentTestEntities {

    private DocumentTestEntities() {
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    public static class TestCatalog extends EntityWithId {

        static final String TYPE = "test-catalog";

        private String name;
        private String description;
        private Instant lastUpdate;
        private List<String> keywords;
        private TimePeriod validFor;
        private Money price;
        private List<Characteristic> characteristic;
        private List<RelatedParty> relatedParty;
        private TestUriRef parent;

        public TestCatalog(String id) {
            super(TYPE, id);
        }
    }

    // like TMF639/TMF730 resources: a concrete base with a sub-type, both id-constructed
    @Getter
    @Setter
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    public static class TestResource extends EntityWithId {

        static final String TYPE = "test-resource";

        private String name;

        public TestResource(String id) {
            super(TYPE, id);
        }

        protected TestResource(String type, String id) {
            super(type, id);
        }
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    public static class TestLogicalResource extends TestResource {

        static final String TYPE = "test-logical-resource";

        private String value;

        public TestLogicalResource(String id) {
            super(TYPE, id);
        }
    }

    // only a URI constructor, and getReferencedTypes without @JsonIgnore, unlike most real references
    @Getter
    @Setter
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    public static class TestUriRef extends RefEntity {

        public TestUriRef(URI id) {
            super(id);
        }

        @Override
        public List<String> getReferencedTypes() {
            return List.of(TestCatalog.TYPE);
        }
    }

    // like customer-bill FinancialAccountRef.accountBalance
    @Getter
    @Setter
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    public static class TestNotStoredHolder extends EntityWithId {

        static final String TYPE = "test-not-stored-holder";

        private String name;
        @NotStored
        private List<Money> balance;

        public TestNotStoredHolder(String id) {
            super(TYPE, id);
        }
    }
}
