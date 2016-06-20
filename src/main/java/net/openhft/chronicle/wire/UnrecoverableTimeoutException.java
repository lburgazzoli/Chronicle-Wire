package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Created by peter on 22/05/16.
 */
public class UnrecoverableTimeoutException extends IllegalStateException {
    public UnrecoverableTimeoutException(@NotNull Exception e) {
        super(e.getMessage());
        initCause(e);
    }
}
