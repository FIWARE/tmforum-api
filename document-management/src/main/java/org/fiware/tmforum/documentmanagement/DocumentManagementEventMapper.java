package org.fiware.tmforum.documentmanagement;

import lombok.RequiredArgsConstructor;
import org.fiware.document.model.DocumentSpecificationVO;
import org.fiware.tmforum.common.exception.TmForumException;
import org.fiware.tmforum.common.exception.TmForumExceptionReason;
import org.fiware.tmforum.common.mapping.EventMapping;
import org.fiware.tmforum.common.notification.EventMapper;
import org.fiware.tmforum.documentmanagement.domain.DocumentSpecification;

import javax.inject.Singleton;
import java.util.Map;

import static java.util.Map.entry;

@RequiredArgsConstructor
@Singleton
public class DocumentManagementEventMapper implements EventMapper {

    private final TMForumMapper tmForumMapper;

    @Override
    public Map<String, EventMapping> getEntityClassMapping() {
        return Map.ofEntries(
                entry(DocumentSpecification.TYPE_DOCUMENT_SPECIFICATION,
                        new EventMapping(DocumentSpecificationVO.class, DocumentSpecification.class))
        );
    }

    @Override
    public Object mapPayload(Object rawPayload, Class<?> rawClass) {
        if (rawClass == DocumentSpecification.class) {
            return tmForumMapper.map((DocumentSpecification) rawPayload);
        }
        throw new TmForumException(
                String.format("Event-Payload %s is not supported.", rawPayload),
                TmForumExceptionReason.INVALID_DATA);
    }
}
