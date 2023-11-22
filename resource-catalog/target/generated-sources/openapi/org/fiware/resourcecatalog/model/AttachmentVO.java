package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class AttachmentVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_HREF = "href";
	public static final java.lang.String JSON_PROPERTY_ATTACHMENT_TYPE = "attachmentType";
	public static final java.lang.String JSON_PROPERTY_CONTENT = "content";
	public static final java.lang.String JSON_PROPERTY_DESCRIPTION = "description";
	public static final java.lang.String JSON_PROPERTY_MIME_TYPE = "mimeType";
	public static final java.lang.String JSON_PROPERTY_NAME = "name";
	public static final java.lang.String JSON_PROPERTY_URL = "url";
	public static final java.lang.String JSON_PROPERTY_SIZE = "size";
	public static final java.lang.String JSON_PROPERTY_VALID_FOR = "validFor";
	public static final java.lang.String JSON_PROPERTY_AT_BASE_TYPE = "@baseType";
	public static final java.lang.String JSON_PROPERTY_AT_SCHEMA_LOCATION = "@schemaLocation";
	public static final java.lang.String JSON_PROPERTY_AT_TYPE = "@type";

	/** Unique identifier for this particular attachment */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String id;

	/** URI for this Attachment */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_HREF)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI href;

	/** Attachment type such as video, picture */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ATTACHMENT_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String attachmentType;

	/** The actual contents of the attachment object, if embedded, encoded as base64 */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CONTENT)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String content;

	/** A narrative text describing the content of the attachment */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_DESCRIPTION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String description;

	/** Attachment mime type such as extension file for video, picture and document */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_MIME_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String mimeType;

	/** The name of the attachment */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_NAME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String name;

	/** Uniform Resource Locator, is a web page address (a subset of URI) */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_URL)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI url;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_SIZE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private QuantityVO size;

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_VALID_FOR)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private TimePeriodVO validFor;

	/** When sub-classing, this defines the super-class */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_BASE_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String atBaseType;

	/** A URI to a JSON-Schema file that defines additional attributes and relationships */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_SCHEMA_LOCATION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.net.URI atSchemaLocation;

	/** When sub-classing, this defines the sub-class Extensible name */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_AT_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String atType;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		AttachmentVO other = (AttachmentVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(href, other.href)
				&& java.util.Objects.equals(attachmentType, other.attachmentType)
				&& java.util.Objects.equals(content, other.content)
				&& java.util.Objects.equals(description, other.description)
				&& java.util.Objects.equals(mimeType, other.mimeType)
				&& java.util.Objects.equals(name, other.name)
				&& java.util.Objects.equals(url, other.url)
				&& java.util.Objects.equals(size, other.size)
				&& java.util.Objects.equals(validFor, other.validFor)
				&& java.util.Objects.equals(atBaseType, other.atBaseType)
				&& java.util.Objects.equals(atSchemaLocation, other.atSchemaLocation)
				&& java.util.Objects.equals(atType, other.atType);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, href, attachmentType, content, description, mimeType, name, url, size, validFor, atBaseType, atSchemaLocation, atType);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("AttachmentVO[")
				.append("id=").append(id).append(",")
				.append("href=").append(href).append(",")
				.append("attachmentType=").append(attachmentType).append(",")
				.append("content=").append(content).append(",")
				.append("description=").append(description).append(",")
				.append("mimeType=").append(mimeType).append(",")
				.append("name=").append(name).append(",")
				.append("url=").append(url).append(",")
				.append("size=").append(size).append(",")
				.append("validFor=").append(validFor).append(",")
				.append("atBaseType=").append(atBaseType).append(",")
				.append("atSchemaLocation=").append(atSchemaLocation).append(",")
				.append("atType=").append(atType)
				.append("]")
				.toString();
	}

	// fluent

	public AttachmentVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public AttachmentVO href(java.net.URI newHref) {
		this.href = newHref;
		return this;
	}

	public AttachmentVO attachmentType(java.lang.String newAttachmentType) {
		this.attachmentType = newAttachmentType;
		return this;
	}

	public AttachmentVO content(java.lang.String newContent) {
		this.content = newContent;
		return this;
	}

	public AttachmentVO description(java.lang.String newDescription) {
		this.description = newDescription;
		return this;
	}

	public AttachmentVO mimeType(java.lang.String newMimeType) {
		this.mimeType = newMimeType;
		return this;
	}

	public AttachmentVO name(java.lang.String newName) {
		this.name = newName;
		return this;
	}

	public AttachmentVO url(java.net.URI newUrl) {
		this.url = newUrl;
		return this;
	}

	public AttachmentVO size(QuantityVO newSize) {
		this.size = newSize;
		return this;
	}

	public AttachmentVO validFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
		return this;
	}

	public AttachmentVO atBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
		return this;
	}

	public AttachmentVO atSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
		return this;
	}

	public AttachmentVO atType(java.lang.String newAtType) {
		this.atType = newAtType;
		return this;
	}

	// getter/setter

	public java.lang.String getId() {
		return id;
	}

	public void setId(java.lang.String newId) {
		this.id = newId;
	}

	public java.net.URI getHref() {
		return href;
	}

	public void setHref(java.net.URI newHref) {
		this.href = newHref;
	}

	public java.lang.String getAttachmentType() {
		return attachmentType;
	}

	public void setAttachmentType(java.lang.String newAttachmentType) {
		this.attachmentType = newAttachmentType;
	}

	public java.lang.String getContent() {
		return content;
	}

	public void setContent(java.lang.String newContent) {
		this.content = newContent;
	}

	public java.lang.String getDescription() {
		return description;
	}

	public void setDescription(java.lang.String newDescription) {
		this.description = newDescription;
	}

	public java.lang.String getMimeType() {
		return mimeType;
	}

	public void setMimeType(java.lang.String newMimeType) {
		this.mimeType = newMimeType;
	}

	public java.lang.String getName() {
		return name;
	}

	public void setName(java.lang.String newName) {
		this.name = newName;
	}

	public java.net.URI getUrl() {
		return url;
	}

	public void setUrl(java.net.URI newUrl) {
		this.url = newUrl;
	}

	public QuantityVO getSize() {
		return size;
	}

	public void setSize(QuantityVO newSize) {
		this.size = newSize;
	}

	public TimePeriodVO getValidFor() {
		return validFor;
	}

	public void setValidFor(TimePeriodVO newValidFor) {
		this.validFor = newValidFor;
	}

	public java.lang.String getAtBaseType() {
		return atBaseType;
	}

	public void setAtBaseType(java.lang.String newAtBaseType) {
		this.atBaseType = newAtBaseType;
	}

	public java.net.URI getAtSchemaLocation() {
		return atSchemaLocation;
	}

	public void setAtSchemaLocation(java.net.URI newAtSchemaLocation) {
		this.atSchemaLocation = newAtSchemaLocation;
	}

	public java.lang.String getAtType() {
		return atType;
	}

	public void setAtType(java.lang.String newAtType) {
		this.atType = newAtType;
	}
}
