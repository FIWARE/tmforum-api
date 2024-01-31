package org.fiware.tmforum.common.notification.checkers.tmforum;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;

@RequiredArgsConstructor
public class StateChangeTmForumEventChecker<T> implements TmForumEventChecker {
    private final T newState;
    private final T oldState;

    @Override
    public boolean wasFired(String query) {
        return hasEntityStateChanged(oldState, newState);
    }

    private boolean hasEntityStateChanged(T oldState, T newState) {
        Object oldStateFieldValue = getEntityState(oldState);
        Object newStateFieldValue = getEntityState(newState);

        return oldStateFieldValue == null && newStateFieldValue != null ||
                oldStateFieldValue != null && newStateFieldValue == null ||
                oldStateFieldValue != null && !oldStateFieldValue.equals(newStateFieldValue) ||
                newStateFieldValue != null && !newStateFieldValue.equals(oldStateFieldValue);
    }

    private Object getEntityState(T entity) {
        Object stateFieldValue;
        try {
            stateFieldValue = entity.getClass().getMethod("getEntityState").invoke(entity);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            stateFieldValue = null;
        }
        return stateFieldValue;
    }
}
