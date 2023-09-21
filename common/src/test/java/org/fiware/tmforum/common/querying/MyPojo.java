package org.fiware.tmforum.common.querying;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.EntityId;
import io.github.wistefan.mapping.annotations.EntityType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.time.Instant;

@MappingEnabled(entityType = "my-pojo")
public class MyPojo {
	private static final String ENTITY_TYPE = "my-pojo";

	private URI id;

	private String color;
	private String status;
	private SubObject sub;
	private SubObject otherNamedSub;
	private Integer temperature;
	private Instant createdAt;

	// required constructor
	public MyPojo(String id) {
		this.id = URI.create(id);
	}

	@EntityId
	public URI getId() {
		return id;
	}

	@EntityType
	public String getType() {
		return ENTITY_TYPE;
	}

	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "color")
	public String getColor() {
		return color;
	}

	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "color")
	public void setColor(String color) {
		this.color = color;
	}

	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "status")
	public String getStatus() {
		return status;
	}

	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "status")
	public void setStatus(String status) {
		this.status = status;
	}

	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "temperature")
	public Integer getTemperature() {
		return temperature;
	}

	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "temperature", targetClass = Integer.class)
	public void setTemperature(Integer temperature) {
		this.temperature = temperature;
	}

	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "sub")
	public SubObject getSub() {
		return sub;
	}

	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "sub", targetClass = SubObject.class)
	public void setSub(SubObject sub) {
		this.sub = sub;
	}

	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "otherSub")
	public SubObject getOtherNamedSub() {
		return otherNamedSub;
	}

	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "otherSub", targetClass = SubObject.class)
	public void setOtherNamedSub(SubObject otherSub) {
		this.otherNamedSub = otherNamedSub;
	}

	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "createdAt")
	public Instant getCreatedAt() {
		return createdAt;
	}

	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "createdAt", targetClass = Instant.class)
	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public MyPojo color(String color) {
		this.color = color;
		return this;
	}

	public MyPojo temperature(Integer temperature) {
		this.temperature = temperature;
		return this;
	}

	public MyPojo createdAt(Instant createdAt) {
		this.createdAt = createdAt;
		return this;
	}
}
