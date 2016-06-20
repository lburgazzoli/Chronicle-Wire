package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by peter on 28/03/16.
 */
public class MethodWriterBuilder<T> implements Supplier<T> {
    private final List<Class> interfaces = new ArrayList<>();
    @NotNull
    private final MethodWriterInvocationHandler handler;
    private ClassLoader classLoader;

    public MethodWriterBuilder(MarshallableOut out, @NotNull Class<T> tClass) {
        interfaces.add(Closeable.class);
        interfaces.add(tClass);
        handler = new MethodWriterInvocationHandler(out);
        classLoader = tClass.getClassLoader();
    }

    @NotNull
    public MethodWriterBuilder<T> classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> addInterface(Class additionalClass) {
        interfaces.add(additionalClass);
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> recordHistory(boolean recordHistory) {
        handler.recordHistory(recordHistory);
        return this;
    }

    @NotNull
    public MethodWriterBuilder<T> onClose(Closeable closeable) {
        handler.onClose(closeable);
        return this;
    }

    @NotNull
    @Override
    public T get() {
        @NotNull Class[] interfacesArr = interfaces.toArray(new Class[interfaces.size()]);
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(classLoader, interfacesArr, handler);
    }
}
