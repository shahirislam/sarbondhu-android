package com.sarbondhu.app.ui.common;

/** A LiveData value that should be acted on once (toast, navigation), not again after rotation. */
public final class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    /** Returns the content the first time, then null. */
    public T take() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }
}
