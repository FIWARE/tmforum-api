package org.fiware.tmforum.migration.postgresql;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * The references that the reference backfill fills, loaded from {@value #RESOURCE}.
 * <p>
 * Entries are the 81 top-level relationship setters that were not {@code fromProperties = true} at commit
 * {@code 9570e548}, the last commit before ticket 88. Pre-88, {@code EntityVOMapper.fromEntityVO} fetched the targets of
 * exactly these attributes ({@code getRelationshipObjects} walks the entity's top-level attributes only) and mapped
 * each target entity into the reference class, with the reference's own stored sub-attributes merged over it. A
 * reference field was therefore filled when the reference class has a non-relationship {@code @AttributeSetter} whose
 * target name is also a setter of a stored class of the reference's {@code @MappingEnabled} type. {@code id} and the
 * JSON-LD typing fields ({@code atType}, {@code atBaseType}, {@code atSchemaLocation}) are left out. A reference class
 * without {@code @MappingEnabled}, or one whose type no stored class has, gets no fillable field. {@code version} is
 * included wherever pre-88 filled it, because that is what clients saw.
 * <p>
 * {@link #excluded()} lists the two non-{@code fromProperties} setters on embedded classes, which were never resolved.
 * {@code ReferenceBackfillCatalogTest} pins the catalog against the domain classes.
 */
public record ReferenceBackfillCatalog(List<Entry> entries, List<Excluded> excluded,
									   List<DatasetIdRewrite> datasetIdRewrites) {

	static final String RESOURCE = "reference-backfill-catalog.json";

	/**
	 * A backfilled relationship attribute. {@code ownerTypes} holds the stored types of the owner class and its subclasses.
	 */
	public record Entry(String ownerClass, List<String> ownerTypes, String attribute, boolean list,
						String referenceClass, List<FillableField> fillableFields) {
	}

	/**
	 * A reference field that is copied from the target document when the stored reference lacks it.
	 */
	public record FillableField(String referenceKey, String targetKey) {
	}

	/**
	 * A non-{@code fromProperties} setter that the backfill leaves alone, with the reason.
	 */
	public record Excluded(String declaringClass, String attribute, String referenceClass, String reason) {
	}

	/**
	 * A broker reference list whose datasetId changed from {@code <productOrderId>} to
	 * {@code <productOrderId>:<orderItemId>}; the broker backfill rewrites it.
	 */
	public record DatasetIdRewrite(String ownerClass, List<String> ownerTypes, String attribute,
								   String referenceClass) {
	}

	/**
	 * Reads the catalog from the classpath. Fails with an unchecked exception when the resource is missing or malformed.
	 */
	public static ReferenceBackfillCatalog load() {
		try (InputStream in = ReferenceBackfillCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource " + RESOURCE);
			}
			return JsonMapper.builder().build().readValue(in, ReferenceBackfillCatalog.class);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot read " + RESOURCE, e);
		}
	}
}
