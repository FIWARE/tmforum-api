package org.fiware.tmforum.resource;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Quantity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A SoftwareSpecification deployed using the SoftwareSupportPackage on a platform
 * which meets the HostingPlatformRequirements.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = InstalledSoftware.TYPE_INSTALLED_SOFTWARE)
public class InstalledSoftware extends SoftwareResource {

	public static final String TYPE_INSTALLED_SOFTWARE = "installed-software";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "isUTCTime") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "isUTCTime") }))
	private Boolean isUTCTime;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastStartTime") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastStartTime") }))
	private Instant lastStartTime;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "numProcessesActiveCurrent") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "numProcessesActiveCurrent") }))
	private Integer numProcessesActiveCurrent;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "numUsersCurrent") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "numUsersCurrent") }))
	private Integer numUsersCurrent;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "serialNumber") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "serialNumber") }))
	private String serialNumber;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "pagingFileSizeCurrent") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "pagingFileSizeCurrent") }))
	private Quantity pagingFileSizeCurrent;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "processMemorySizeCurrent") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "processMemorySizeCurrent") }))
	private Quantity processMemorySizeCurrent;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "swapSpaceUsedCurrent") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "swapSpaceUsedCurrent") }))
	private Quantity swapSpaceUsedCurrent;

	/**
	 * Create a new InstalledSoftware.
	 *
	 * @param id the entity id
	 */
	public InstalledSoftware(String id) {
		super(TYPE_INSTALLED_SOFTWARE, id);
	}

	@Override
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of(TYPE_INSTALLED_SOFTWARE));
	}
}
