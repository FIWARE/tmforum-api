package org.fiware.tmforum.resource;

import lombok.Data;

import java.net.URI;
import java.time.Instant;

@Data
public class Note {

    private URI noteId;
    private URI href;
    private String author;
    private Instant date;
    private String text;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;

}
