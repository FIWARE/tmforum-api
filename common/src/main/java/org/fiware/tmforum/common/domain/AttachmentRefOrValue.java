package org.fiware.tmforum.common.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.Quantity;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;
import java.net.URL;

//TODO: Make sense of this.
@EqualsAndHashCode(callSuper = true)
@Data
public class AttachmentRefOrValue extends Entity {

    private URI attachementId;
    private URI href;
    private String attachmentType;
    private String content;
    private String description;
    private String mimeType;
    private URL url;
    private Quantity size;
    private TimePeriod validFor;
    private String name;
    private String atReferredType;
}
