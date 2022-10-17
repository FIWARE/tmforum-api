package org.fiware.tmforum.resourcecatalog.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.net.URI;
import java.time.Instant;

@Data
public class Note {

    private URI id;
    private String author;
    private Instant date;
    private String text;
    @JsonAlias("@baseType")
    private String atBaseType;
    @JsonAlias("@schemaLocation")
    private URI atSchemaLocation;
    @JsonAlias("@type")
    private String atType;

}
