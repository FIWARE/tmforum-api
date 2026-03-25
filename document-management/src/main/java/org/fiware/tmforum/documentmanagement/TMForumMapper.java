package org.fiware.tmforum.documentmanagement;

import io.github.wistefan.mapping.MappingException;
import org.fiware.document.model.*;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.common.mapping.BaseMapper;
import org.fiware.tmforum.common.mapping.IdHelper;
import org.fiware.tmforum.documentmanagement.domain.DocumentSpecification;
import org.fiware.tmforum.service.CharacteristicSpecification;
import org.fiware.tmforum.service.CharacteristicValueSpecification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@Mapper(componentModel = "jsr330", uses = {IdHelper.class})
public abstract class TMForumMapper extends BaseMapper {

    // DocumentSpecification mappings

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract DocumentSpecificationVO map(DocumentSpecificationCreateVO createVO, URI id);

    public abstract DocumentSpecificationVO map(DocumentSpecification documentSpecification);

    @Mapping(target = "href", source = "id")
    public abstract DocumentSpecification map(DocumentSpecificationVO documentSpecificationVO);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "href", source = "id")
    public abstract DocumentSpecificationVO map(DocumentSpecificationUpdateVO updateVO, String id);

    // CharacteristicSpecification mappings

    @Mapping(target = "id", source = "tmfId")
    public abstract CharacteristicSpecificationVO map(CharacteristicSpecification characteristicSpecification);

    @Mapping(target = "tmfId", source = "id")
    public abstract CharacteristicSpecification map(CharacteristicSpecificationVO characteristicSpecificationVO);

    @Mapping(target = "tmfValue", source = "value")
    public abstract CharacteristicValueSpecification map(CharacteristicValueSpecificationVO characteristicVO);

    @Mapping(target = "value", source = "tmfValue")
    public abstract CharacteristicValueSpecificationVO map(CharacteristicValueSpecification characteristic);

    // AttachmentRefOrValue mappings

    public abstract AttachmentRefOrValue map(AttachmentRefOrValueVO attachmentVO);

    public abstract AttachmentRefOrValueVO map(AttachmentRefOrValue attachment);

    // URL/URI helper methods

    public URL map(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new MappingException(String.format("%s is not a URL.", value), e);
        }
    }

    public String map(URL value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public URI map(URI value) {
        return value;
    }

    public URI mapToURI(String value) {
        if (value == null) {
            return null;
        }
        return URI.create(value);
    }

    public String mapFromURI(URI value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}
