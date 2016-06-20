package net.openhft.chronicle.wire;

import org.jetbrains.annotations.Nullable;

/**
 * Created by peter on 10/05/16.
 */
public interface SerializationStrategy<T> {
    @Nullable
    default T read(ValueIn in, Class<T> type) {
        return readUsing(newInstance(type), in);
    }

    @Nullable
    default T readUsing(@Nullable T using, ValueIn in, @Nullable Class<T> type) {
        if (using == null && type != null)
            using = newInstance(type);
        return readUsing(using, in);
    }

    @Nullable
    T readUsing(T using, ValueIn in);

    T newInstance(Class<T> type);

    Class<T> type();

    BracketType bracketType();
}
