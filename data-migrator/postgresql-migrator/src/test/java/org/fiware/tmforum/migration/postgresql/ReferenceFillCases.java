package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.migration.postgresql.ReferenceBackfillCatalog.FillableField;

import java.util.List;
import java.util.stream.Stream;

/**
 * The fill-absent cases that {@link ReferenceFiller} and its SQL twin {@code pg_temp.fill_ref} must both pass.
 */
final class ReferenceFillCases {

	static final List<FillableField> FIELDS = List.of(new FillableField("name", "name"),
			new FillableField("href", "href"), new FillableField("version", "version"),
			new FillableField("validFor", "validFor"), new FillableField("label", "title"));

	private static final String TARGET = """
			{"id": "urn:t", "name": "target", "href": "http://t.example.org", "version": "1.0",
			 "validFor": {"startDateTime": "2020-01-01T00:00:00Z"}, "title": "a title", "description": "not fillable"}""";

	private ReferenceFillCases() {
	}

	/**
	 * A reference and its target document ({@code null}: no target) and the reference the fill must return.
	 */
	record Case(String name, String reference, String target, String expected) {

		@Override
		public String toString() {
			return name;
		}
	}

	static Stream<Case> cases() {
		return Stream.of(
				new Case("absent fields are filled", """
						{"id": "urn:t"}""", TARGET, """
						{"id": "urn:t", "name": "target", "href": "http://t.example.org", "version": "1.0",
						 "validFor": {"startDateTime": "2020-01-01T00:00:00Z"}, "label": "a title"}"""),
				new Case("a client-sent field is kept", """
						{"id": "urn:t", "name": "client name", "version": "0.9"}""", TARGET, """
						{"id": "urn:t", "name": "client name", "version": "0.9", "href": "http://t.example.org",
						 "validFor": {"startDateTime": "2020-01-01T00:00:00Z"}, "label": "a title"}"""),
				new Case("a JSON null in the reference counts as present", """
						{"id": "urn:t", "name": null}""", TARGET, """
						{"id": "urn:t", "name": null, "href": "http://t.example.org", "version": "1.0",
						 "validFor": {"startDateTime": "2020-01-01T00:00:00Z"}, "label": "a title"}"""),
				new Case("null and absent target values are never copied", """
						{"id": "urn:t"}""", """
						{"id": "urn:t", "name": null, "version": "2.0"}""", """
						{"id": "urn:t", "version": "2.0"}"""),
				new Case("a missing target leaves the reference unchanged", """
						{"id": "urn:t"}""", null, """
						{"id": "urn:t"}"""),
				new Case("a complete reference is unchanged", """
						{"id": "urn:t", "name": "n", "href": "h", "version": "v", "validFor": {}, "label": "l"}""",
						TARGET, """
						{"id": "urn:t", "name": "n", "href": "h", "version": "v", "validFor": {}, "label": "l"}"""),
				new Case("a non-object reference is unchanged", """
						"urn:t\"""", TARGET, """
						"urn:t\""""));
	}
}
