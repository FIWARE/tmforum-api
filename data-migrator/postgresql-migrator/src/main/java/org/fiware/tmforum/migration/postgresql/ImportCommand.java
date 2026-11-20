package org.fiware.tmforum.migration.postgresql;

import lombok.extern.slf4j.Slf4j;
import org.fiware.ngsi.model.EntityVO;
import org.fiware.tmforum.common.domain.VersionedEntity;
import org.fiware.tmforum.migration.postgresql.ImportReport.TypeReport;
import org.fiware.tmforum.storage.StorageErrorReason;
import org.fiware.tmforum.storage.StorageException;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Copies every entity of the requested types from the source into the target, type by type, page by page.
 * Broker paging has no stable order, so an id read twice is written once, and a type whose distinct ids fall short of
 * the broker's count fails; a {@code --resume} run then picks up the rest.
 */
@Slf4j
class ImportCommand {

	// the label a versioned entity without a version gets, so its document and version column agree
	static final String SEED_VERSION = "0.0.1";
	private static final String VERSION_SETTER = "setVersion";

	private final EntitySource source;
	private final EntityTarget target;
	private final EntityTypeRegistry registry;
	private final ImportOptions options;

	ImportCommand(EntitySource source, EntityTarget target, EntityTypeRegistry registry, ImportOptions options) {
		this.source = source;
		this.target = target;
		this.registry = registry;
		this.options = options;
	}

	/**
	 * The requested types, or every registry type when none is requested. Throws {@link IllegalArgumentException} for
	 * a type the registry does not map.
	 */
	static Set<String> resolveTypes(EntityTypeRegistry registry, List<String> requested) {
		if (requested == null || requested.isEmpty()) {
			return registry.types();
		}
		Set<String> types = new TreeSet<>(requested);
		Set<String> unknown = registry.unmappedTypes(types);
		if (!unknown.isEmpty()) {
			throw new IllegalArgumentException("No domain class maps the requested types " + unknown + ".");
		}
		return types;
	}

	ImportReport run(Collection<String> types) {
		ImportReport report = new ImportReport(options.dryRun());
		try {
			report.unmappedTypes(registry.unmappedTypes(source.types()));
		} catch (RuntimeException e) {
			log.warn("Was not able to list the source's types.", e);
			report.typeListingFailed(e.getMessage());
		}
		for (String type : types) {
			TypeReport typeReport = report.type(type);
			try {
				importType(type, typeReport);
			} catch (RuntimeException e) {
				log.warn("Was not able to import {}.", type, e);
				typeReport.error = e.getMessage();
			}
			if (options.failFast() && !typeReport.succeeded()) {
				report.abort();
				break;
			}
		}
		return report;
	}

	private void importType(String type, TypeReport report) {
		if (!options.resume()) {
			long stored = target.count(type);
			if (stored > 0) {
				report.error = String.format("the target already holds %d entities of this type; use --resume to "
						+ "continue an earlier import.", stored);
				return;
			}
		}
		Class<?> domainClass = registry.domainClass(type)
				.orElseThrow(() -> new IllegalArgumentException("No domain class maps " + type + "."));
		Set<URI> seen = new HashSet<>();
		int offset = 0;
		long brokerCount;
		do {
			EntitySource.Page page = source.page(type, offset, options.pageSize());
			brokerCount = page.totalCount();
			for (EntityVO entity : page.entities()) {
				report.read++;
				if (!seen.add(entity.getId())) {
					report.duplicateRead++;
					continue;
				}
				if (!importEntity(entity, domainClass, report) && options.failFast()) {
					return;
				}
			}
			if (page.entities().isEmpty()) {
				break;
			}
			// advancing by what came back, not by the page size, so a broker capping the limit skips nothing
			offset += page.entities().size();
		} while (offset < brokerCount);
		report.brokerCount = brokerCount;
		if (seen.size() < brokerCount) {
			report.error = String.format("read %d distinct ids, but the broker counts %d; %d were skipped by unstable "
					+ "paging or concurrent writes. Freeze writes and re-run with --resume.",
					seen.size(), brokerCount, brokerCount - seen.size());
		}
	}

	private boolean importEntity(EntityVO entity, Class<?> domainClass, TypeReport report) {
		Object domainEntity;
		try {
			domainEntity = source.toDomain(entity, domainClass);
			if (seedVersion(domainEntity)) {
				report.seededVersion++;
			}
		} catch (RuntimeException e) {
			log.warn("Was not able to map {}.", entity.getId(), e);
			report.failed++;
			return false;
		}
		if (options.dryRun()) {
			report.written++;
			return true;
		}
		try {
			target.importEntity(domainEntity, entity.getCreatedAt(), entity.getModifiedAt());
			report.written++;
			return true;
		} catch (StorageException e) {
			if (options.resume() && e.reason() == StorageErrorReason.CONFLICT) {
				report.skipped++;
				return true;
			}
			log.warn("Was not able to write {}.", entity.getId(), e);
		} catch (RuntimeException e) {
			log.warn("Was not able to write {}.", entity.getId(), e);
		}
		report.failed++;
		return false;
	}

	private static boolean seedVersion(Object domainEntity) {
		if (!(domainEntity instanceof VersionedEntity versioned)
				|| (versioned.getVersion() != null && !versioned.getVersion().isBlank())) {
			return false;
		}
		// every VersionedEntity has a Lombok setter, the interface only declares the getter
		try {
			domainEntity.getClass().getMethod(VERSION_SETTER, String.class).invoke(domainEntity, SEED_VERSION);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Was not able to seed the version of " + domainEntity.getClass(), e);
		}
		return true;
	}
}
