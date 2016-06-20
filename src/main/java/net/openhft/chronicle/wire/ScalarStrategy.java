package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by peter on 10/05/16.
 */
class ScalarStrategy<T> implements SerializationStrategy<T> {
    final BiFunction<T, ValueIn, T> read;
    private final Class<T> type;

    ScalarStrategy(Class<T> type, BiFunction<T, ValueIn, T> read) {
        this.type = type;
        this.read = read;
    }

    @NotNull
    static <T> ScalarStrategy<T> of(Class<T> clazz, BiFunction<T, ValueIn, T> read) {
        return new ScalarStrategy<>(clazz, read);
    }

    @Nullable
    static <T> ScalarStrategy<T> text(Class<T> clazz, @NotNull Function<String, T> func) {
        return new ScalarStrategy<>(clazz, (o, in) -> {
            @Nullable String text = in.text();
            return text == null ? null : func.apply(text);
        });
    }

    @NotNull
    @Override
    public BracketType bracketType() {
        return BracketType.NONE;
    }

    @Override
    public T newInstance(Class type) {
        return ObjectUtils.newInstance(this.type);
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Nullable
    @Override
    public T readUsing(T using, @NotNull ValueIn in) {
        if (in.isNull())
            return null;
        return read.apply(using, in);
    }

    @NotNull
    @Override
    public String toString() {
        return "ScalarStrategy<" + type.getName() + ">";
    }
}
