/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.VanillaBytes;
import net.openhft.chronicle.core.ClassLocal;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Externalizable;
import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Created by peter on 31/08/15.
 */
public enum Wires {
    ;
    public static final int LENGTH_MASK = -1 >>> 2;
    public static final int NOT_COMPLETE = 1 << 31;
    @Deprecated
    public static final int NOT_READY = NOT_COMPLETE;
    public static final int META_DATA = 1 << 30;
    public static final int UNKNOWN_LENGTH = 0x0;
    public static final int MAX_LENGTH = (1 << 30) - 1;

    // value to use when the message is not ready and of an unknown length
    public static final int NOT_COMPLETE_UNKNOWN_LENGTH = NOT_COMPLETE | UNKNOWN_LENGTH;
    // value to use when no more data is possible e.g. on a roll.
    public static final int END_OF_DATA = NOT_COMPLETE | META_DATA | UNKNOWN_LENGTH;

    public static final int NOT_INITIALIZED = 0x0;
    public static final Bytes<?> NO_BYTES = new VanillaBytes<>(BytesStore.empty());
    public static final WireIn EMPTY = new BinaryWire(NO_BYTES);
    public static final int SPB_HEADER_SIZE = 4;
    public static final List<Function<Class, SerializationStrategy>> CLASS_STRATEGY_FUNCTIONS = new CopyOnWriteArrayList<>();
    public static final ClassLocal<SerializationStrategy> CLASS_STRATEGY = ClassLocal.withInitial(c -> {
        for (@NotNull Function<Class, SerializationStrategy> func : CLASS_STRATEGY_FUNCTIONS) {
            final SerializationStrategy strategy = func.apply(c);
            if (strategy != null)
                return strategy;
        }
        return SerializationStrategies.ANY_OBJECT;
    });
    static final StringBuilderPool SBP = new StringBuilderPool();

    static {
        CLASS_STRATEGY_FUNCTIONS.add(new SerializeJavaLang());
        CLASS_STRATEGY_FUNCTIONS.add(new SerializeMarshallables());
        CLASS_STRATEGY_FUNCTIONS.add(new SerializeBytes());
    }

    public static <T> T read(@NotNull Class<T> tClass, ValueIn in) {
        final SerializationStrategy<T> strategy = CLASS_STRATEGY.get(tClass);
        return strategy.read(in, tClass);
    }

    /**
     * This decodes some Bytes where the first 4-bytes is the length.  e.g. Wire.writeDocument wrote
     * it. <a href="https://github.com/OpenHFT/RFC/tree/master/Size-Prefixed-Blob">Size Prefixed
     * Blob</a>
     *
     * @param bytes to decode
     * @return as String
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes) {
        long position = bytes.readPosition();
        return WireInternal.fromSizePrefixedBlobs(bytes, position, bytes.readRemaining());
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes, long position) {
        final long limit = bytes.readLimit();
        if (position > limit)
            return "";
        return WireInternal.fromSizePrefixedBlobs(bytes, position, limit - position);
    }

    public static String fromSizePrefixedBlobs(@NotNull DocumentContext dc) {
        return Wires.fromSizePrefixedBlobs(dc.wire().bytes(), dc.wire().bytes().readPosition() - 4);
    }

    public static String fromSizePrefixedBlobs(@NotNull WireIn wireIn) {
        @NotNull final Bytes<?> bytes = wireIn.bytes();
        long position = bytes.readPosition();
        return WireInternal.fromSizePrefixedBlobs(bytes, position, bytes.readRemaining());
    }

    public static CharSequence asText(@NotNull WireIn wireIn) {
        long pos = wireIn.bytes().readPosition();
        try {
            Bytes bytes = acquireBytes();
            wireIn.copyTo(new TextWire(bytes));
            return bytes;
        } finally {
            wireIn.bytes().readPosition(pos);
        }
    }

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    @Deprecated
    public static int lengthOf(long len) {
        return lengthOf((int) len);
    }

    public static int lengthOf(int len) {
        return len & LENGTH_MASK;
    }

    public static boolean isReady(int len) {
        return (len & NOT_COMPLETE) == 0;
    }

    public static boolean isNotComplete(int len) {
        return (len & NOT_COMPLETE) != 0;
    }

    public static boolean isReadyData(int len) {
        return (len & (META_DATA | NOT_COMPLETE)) == 0;
    }

    @Deprecated
    public static boolean isData(long len) {
        return isData((int) len);
    }

    public static boolean isData(int len) {
        return (len & META_DATA) == 0;
    }

    public static boolean isReadyMetaData(int len) {
        return (len & (META_DATA | NOT_COMPLETE)) == META_DATA;
    }

    public static boolean isKnownLength(int len) {
        return (len & (META_DATA | LENGTH_MASK)) != UNKNOWN_LENGTH;
    }

    public static boolean isNotInitialized(int len) {
        return len == NOT_INITIALIZED;
    }

    public static int toIntU30(long l, @NotNull String error) {
        if (l < 0 || l > LENGTH_MASK)
            throw new IllegalStateException(String.format(error, l));
        return (int) l;
    }

    public static boolean acquireLock(@NotNull BytesStore store, long position) {
        return store.compareAndSwapInt(position, NOT_INITIALIZED, NOT_COMPLETE);
    }

    public static boolean exceedsMaxLength(long length) {
        return length > LENGTH_MASK;
    }

    @ForceInline
    public static <T extends ReadMarshallable> long readData(
            @NotNull WireIn wireIn,
            @NotNull T reader) {

        // We assume that check on data readiness and type has been done by the
        // caller
        return readWire(wireIn, reader);
    }

    @ForceInline
    public static <T extends WriteMarshallable> long writeData(
            @NotNull WireOut wireOut,
            @NotNull T writer) {
        return WireInternal.writeData(wireOut, false, false, writer);
    }

    @ForceInline
    public static <T extends WriteMarshallable> long writeMeta(
            @NotNull WireOut wireOut,
            @NotNull T writer) {

        return WireInternal.writeData(wireOut, true, false, writer);
    }

    @ForceInline
    public static <T extends ReadMarshallable> long readMeta(
            @NotNull WireIn wireIn,
            @NotNull T reader) {

        // We assume that check on meta-data readiness and type has been done by
        // the caller
        return readWire(wireIn, reader);
    }

    @ForceInline
    public static long readWire(@NotNull WireIn wireIn, @NotNull ReadMarshallable readMarshallable) {
        @NotNull final Bytes<?> bytes = wireIn.bytes();
        final int header = bytes.readVolatileInt(bytes.readPosition());
        final int len = Wires.lengthOf(header);

        bytes.readSkip(4);

        return readWire(wireIn, len, readMarshallable);
    }

    @ForceInline
    public static long readWire(@NotNull WireIn wireIn, long size, @NotNull ReadMarshallable readMarshallable) {
        @NotNull final Bytes<?> bytes = wireIn.bytes();
        final long limit0 = bytes.readLimit();
        final long limit = bytes.readPosition() + size;
        try {
            bytes.readLimit(limit);
            readMarshallable.readMarshallable(wireIn);
        } finally {
            bytes.readLimit(limit0);
            bytes.readPosition(limit);
        }

        return bytes.readPosition();
    }

    public static Bytes acquireBytes() {
        Bytes bytes = WireInternal.BYTES_TL.get();
        bytes.clear();
        return bytes;
    }

    public static Wire acquireBinaryWire() {
        Wire wire = WireInternal.BINARY_WIRE_TL.get();
        wire.clear();
        return wire;
    }

    public static Bytes acquireAnotherBytes() {
        Bytes bytes = WireInternal.ABYTES_TL.get();
        bytes.clear();
        return bytes;
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes<?> bytes, long position, long length) {
        return WireInternal.fromSizePrefixedBlobs(bytes, position, length);
    }

    public static void readMarshallable(@NotNull Object marshallable, WireIn wire, boolean overwrite) {
        WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass())
                .readMarshallable(marshallable, wire, overwrite);
    }

    public static void writeMarshallable(@NotNull Object marshallable, WireOut wire) {
        WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass())
                .writeMarshallable(marshallable, wire);
    }

    public static void writeMarshallable(@NotNull Object marshallable, WireOut wire, @NotNull Object previous, boolean copy) {
        assert marshallable.getClass() == previous.getClass();
        WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass())
                .writeMarshallable(marshallable, wire, previous, copy);
    }

    public static void writeKey(@NotNull Object marshallable, Bytes bytes) {
        WireMarshaller.WIRE_MARSHALLER_CL.get(marshallable.getClass())
                .writeKey(marshallable, bytes);
    }

    @NotNull
    public static <T extends Marshallable> T deepCopy(@NotNull T marshallable) {
        Wire wire = acquireBinaryWire();
        marshallable.writeMarshallable(wire);
        @NotNull T t = (T) ObjectUtils.newInstance(marshallable.getClass());
        t.readMarshallable(wire);
        return t;
    }

    public static <T> T copyTo(Object marshallable, T t) {
        Wire wire = acquireBinaryWire();
        if (marshallable instanceof WriteMarshallable)
            ((WriteMarshallable) marshallable).writeMarshallable(wire);
        else
            writeMarshallable(marshallable, wire);
        if (t instanceof ReadMarshallable)
            ((ReadMarshallable) t).readMarshallable(wire);
        else
            readMarshallable(t, wire, true);
        return t;
    }

    public static <T> T project(Class<T> tClass, Object obj) {
        T t = ObjectUtils.newInstance(tClass);
        Wires.copyTo(obj, t);
        return t;
    }

    public static boolean isEquals(@NotNull Object o1, @NotNull Object o2) {
        if (o1.getClass() != o2.getClass())
            return false;
        return WireMarshaller.WIRE_MARSHALLER_CL.get(o1.getClass())
                .isEqual(o1, o2);
    }

    static class SerializeBytes implements Function<Class, SerializationStrategy> {
        @Nullable
        @Override
        public SerializationStrategy apply(@NotNull Class aClass) {
            switch (aClass.getName()) {
                case "net.openhft.chronicle.bytes.BytesStore":
                    return ScalarStrategy.of(BytesStore.class, (o, in) -> in.bytesStore());
                default:
                    return null;
            }
        }
    }

    static class SerializeJavaLang implements Function<Class, SerializationStrategy> {
        @Nullable
        @Override
        public SerializationStrategy apply(@NotNull Class aClass) {
            switch (aClass.getName()) {
                case "[B":
                    return ScalarStrategy.of(byte[].class, (o, in) -> in.bytes());

                case "java.lang.StringBuilder":
                    return ScalarStrategy.of(StringBuilder.class, (o, in) -> {
                        StringBuilder builder = (o == null)
                                ? acquireStringBuilder()
                                : o;
                        in.textTo(builder);
                        return o;
                    });

                case "java.lang.String":
                    return ScalarStrategy.of(String.class, (o, in) -> in.text());

                case "java.lang.Object":
                    return SerializationStrategies.ANY_OBJECT;

                case "java.lang.Class":
                    return ScalarStrategy.of(Class.class, (o, in) -> {
                        try {
                            return ClassAliasPool.CLASS_ALIASES.forName(in.text());
                        } catch (ClassNotFoundException e) {
                            throw new IORuntimeException(e);
                        }
                    });

                case "java.lang.Boolean":
                    return ScalarStrategy.of(Boolean.class, (o, in) -> in.bool());

                case "java.lang.Byte":
                    return ScalarStrategy.of(Byte.class, (o, in) -> in.int8());

                case "java.lang.Short":
                    return ScalarStrategy.of(Short.class, (o, in) -> in.int16());

                case "java.lang.Character":
                    return ScalarStrategy.of(Character.class, (o, in) -> {
                        //noinspection unchecked
                        @Nullable final String text = in.text();
                        if (text == null || text.length() == 0)
                            return null;
                        return text.charAt(0);
                    });

                case "java.lang.Integer":
                    return ScalarStrategy.of(Integer.class, (o, in) -> in.int32());

                case "java.lang.Float":
                    return ScalarStrategy.of(Float.class, (o, in) -> in.float32());

                case "java.lang.Long":
                    return ScalarStrategy.of(Long.class, (o, in) -> in.int64());

                case "java.lang.Double":
                    return ScalarStrategy.of(Double.class, (o, in) -> in.float64());

                case "java.time.LocalTime":
                    return ScalarStrategy.of(LocalTime.class, (o, in) -> in.time());

                case "java.time.LocalDate":
                    return ScalarStrategy.of(LocalDate.class, (o, in) -> in.date());

                case "java.time.LocalDateTime":
                    return ScalarStrategy.of(LocalDateTime.class, (o, in) -> in.dateTime());

                case "java.time.ZonedDateTime":
                    return ScalarStrategy.of(ZonedDateTime.class, (o, in) -> in.zonedDateTime());

                case "java.io.File":
                    return ScalarStrategy.text(File.class, File::new);

                case "java.util.UUID":
                    return ScalarStrategy.of(UUID.class, (o, in) -> in.uuid());

                case "java.math.BigInteger":
                    return ScalarStrategy.text(BigInteger.class, BigInteger::new);

                case "java.math.BigDecimal":
                    return ScalarStrategy.text(BigDecimal.class, BigDecimal::new);

                default:
                    if (aClass.isArray()) {
                        final Class componentType = aClass.getComponentType();
                        if (componentType.isPrimitive())
                            return SerializationStrategies.PRIM_ARRAY;
                        return SerializationStrategies.ARRAY;
                    }
                    if (Enum.class.isAssignableFrom(aClass))
                        return SerializationStrategies.ENUM;
                    return null;
            }
        }
    }

    static class SerializeMarshallables implements Function<Class, SerializationStrategy> {
        @Nullable
        @Override
        public SerializationStrategy apply(@NotNull Class aClass) {
            if (Demarshallable.class.isAssignableFrom(aClass))
                return SerializationStrategies.DEMARSHALLABLE;
            if (ReadMarshallable.class.isAssignableFrom(aClass))
                return SerializationStrategies.MARSHALLABLE;
            if (Map.class.isAssignableFrom(aClass))
                return SerializationStrategies.MAP;
            if (Set.class.isAssignableFrom(aClass))
                return SerializationStrategies.SET;
            if (List.class.isAssignableFrom(aClass))
                return SerializationStrategies.LIST;
            if (Externalizable.class.isAssignableFrom(aClass))
                return SerializationStrategies.EXTERNALIZABLE;
            if (Serializable.class.isAssignableFrom(aClass))
                return SerializationStrategies.ANY_NESTED;
            return null;
        }
    }
}
