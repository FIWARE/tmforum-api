package org.fiware.tmforum.common.notification.checkers.ngsild;

import org.fiware.ngsi.model.EntityVO;

public record DeleteNgsiLdEventChecker(String eventType) implements NgsiLdEventChecker {

    @Override
    public boolean wasFired(EntityVO entityVO) {
        return entityVO.getDeletedAt() != null;
    }
}
