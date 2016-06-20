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
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.bytes.ref.BinaryLongReference;
import net.openhft.chronicle.bytes.ref.TextLongArrayReference;
import net.openhft.chronicle.bytes.ref.TextLongReference;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.LicenceCheck;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A selection of prebuilt wire types.
 */
public enum WireType implements Function<Bytes, Wire>, LicenceCheck {

    TEXT {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new TextWire(bytes);
        }

        @Override
        public Supplier<LongValue> newLongReference() {
            return TextLongReference::new;
        }

        @Override
        public Supplier<LongArrayValues> newLongArrayReference() {
            return TextLongArrayReference::new;
        }

    }, BINARY {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new BinaryWire(bytes);
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    }, DEFAULT_ZERO_BINARY {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {

            try {
                return (Wire) Class.forName("software.chronicle.wire.DefaultZeroWire")
                        .getDeclaredConstructor(Bytes.class)
                        .newInstance(bytes);

            } catch (Exception e) {
                @NotNull IllegalStateException licence = new IllegalStateException(
                        "A Chronicle Wire Enterprise licence is required to run this code " +
                                "because you are using DefaultZeroWire which is a licence product. " +
                                "Please contact sales@chronicle.software");
                Jvm.warn().on(getClass(), licence);
                throw licence;
            }
        }

        public void licenceCheck() {
            if (isAvailable())
                return;

            @NotNull final IllegalStateException licence = new IllegalStateException("A Chronicle Wire " +
                    "Enterprise licence is required to run this code because you are using " +
                    "DEFAULT_ZERO_BINARY which is a licence product. " +
                    "Please contact sales@chronicle.software");
            Jvm.warn().on(getClass(), licence);
            throw licence;
        }

        private Boolean isAvailable;

        public boolean isAvailable() {
            if (isAvailable != null)
                return isAvailable;

            try {
                Class e = Class.forName("software.chronicle.wire.DefaultZeroWire");
                e.getDeclaredConstructor(Bytes.class);
                isAvailable = true;
                return true;
            } catch (Exception var4) {
                isAvailable = false;
                return false;
            }
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    }, DELTA_BINARY {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {

            try {
                @NotNull Class<Wire> aClass = (Class) Class.forName("software.chronicle.wire.DeltaWire");
                final Constructor<Wire> declaredConstructor = aClass.getDeclaredConstructor(Bytes.class);
                return declaredConstructor.newInstance(bytes);

            } catch (Exception e) {
                licenceCheck();

                // this should never happen
                throw new AssertionError(e);
            }
        }

        public void licenceCheck() {
            if (isAvailable())
                return;

            @NotNull final IllegalStateException licence = new IllegalStateException("A Chronicle Wire " +
                    "Enterprise licence is required to run this code because you are using " +
                    "DELTA_BINARY which is a licence product. " +
                    "Please contact sales@chronicle.software");
            LOG.error("", licence);
            throw licence;
        }

        private final boolean isAvailable = isAvailable0();

        private boolean isAvailable0() {
            boolean isAvailable;
            try {
                Class.forName("software.chronicle.wire.DeltaWire")
                        .getDeclaredConstructor(Bytes.class);

                return true;

            } catch (Exception fallback) {
                return false;
            }
        }

        public boolean isAvailable() {
            return isAvailable;
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }

    }, FIELDLESS_BINARY {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new BinaryWire(bytes, false, false, true, Integer.MAX_VALUE, "binary");
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    }, COMPRESSED_BINARY {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new BinaryWire(bytes, false, false, false, COMPRESSED_SIZE, "lzw");
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    }, JSON {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new JSONWire(bytes);
        }
    }, RAW {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new RawWire(bytes);
        }

        @NotNull
        @Override
        public String asString(Object marshallable) {
            return asHexString(marshallable);
        }

        @Nullable
        @Override
        public <T> T fromString(@NotNull CharSequence cs) {
            return fromHexString(cs);
        }
    }, CSV {
        @NotNull
        @Override
        public Wire apply(Bytes bytes) {
            return new CSVWire(bytes);
        }
    },
    READ_ANY {
        @NotNull
        @Override
        public Wire apply(@NotNull Bytes bytes) {
            return new ReadAnyWire(bytes);
        }
    };

    static final ThreadLocal<Bytes> bytesTL = ThreadLocal.withInitial(Bytes::allocateElasticDirect);
    static final ThreadLocal<Bytes> bytes2TL = ThreadLocal.withInitial(Bytes::allocateElasticDirect);
    private static final Logger LOG = LoggerFactory.getLogger(WireType.class);
    private static final int COMPRESSED_SIZE = Integer.getInteger("WireType.compressedSize", 128);

    static Bytes getBytes() {
        // when in debug, the output becomes confused if you reuse the buffer.
        if (Jvm.isDebug())
            return Bytes.allocateElasticDirect();
        Bytes bytes = bytesTL.get();
        bytes.clear();
        return bytes;
    }

    static Bytes getBytes2() {
        // when in debug, the output becomes confused if you reuse the buffer.
        if (Jvm.isDebug())
            return Bytes.allocateElasticDirect();
        Bytes bytes = bytes2TL.get();
        bytes.clear();
        return bytes;
    }

    @NotNull
    public static WireType valueOf(Wire wire) {

        if (wire instanceof AbstractAnyWire)
            wire = ((AbstractAnyWire) wire).underlyingWire();

        if (wire instanceof JSONWire)
            return WireType.JSON;

        if (wire instanceof TextWire)
            return WireType.TEXT;

        if ("DeltaWire".equals(wire.getClass().getSimpleName())) {
            return DELTA_BINARY;
        }

        // this must be above BinaryWire
        if ("DefaultZeroWire".equals(wire.getClass().getSimpleName())) {
            return DEFAULT_ZERO_BINARY;
        }

        if (wire instanceof BinaryWire) {
            @NotNull BinaryWire binaryWire = (BinaryWire) wire;
            return binaryWire.fieldLess() ? FIELDLESS_BINARY : WireType.BINARY;
        }

        if (wire instanceof RawWire) {
            return WireType.RAW;
        }

        throw new IllegalStateException("unknown type");
    }

    public Supplier<LongValue> newLongReference() {
        return BinaryLongReference::new;
    }

    public Supplier<LongArrayValues> newLongArrayReference() {
        return BinaryLongArrayReference::new;
    }

    public String asString(Object marshallable) {
        Bytes bytes = asBytes(marshallable);
        return bytes.toString();
    }

    private Bytes asBytes(Object marshallable) {
        Bytes bytes = getBytes();
        Wire wire = apply(bytes);
        @NotNull final ValueOut valueOut = wire.getValueOut();

        if (marshallable instanceof WriteMarshallable)
            valueOut.typedMarshallable((WriteMarshallable) marshallable);
        else if (marshallable instanceof Map)
            wire.getValueOut().marshallable((Map) marshallable, Object.class, Object.class, false);
        else if (marshallable instanceof Iterable)
            wire.getValueOut().sequence((Iterable) marshallable);
        else if (marshallable instanceof Serializable)
            valueOut.typedMarshallable((Serializable) marshallable);
        else
            bytes.appendUtf8(marshallable.toString());
        return bytes;
    }

    @Nullable
    public <T> T fromString(CharSequence cs) {
        Bytes bytes = getBytes2();
        bytes.appendUtf8(cs);
        Wire wire = apply(bytes);
        return (T) wire.getValueIn()
                .object();
    }

    @Nullable
    public <T> T fromFile(String filename) throws IOException {
        return (T) (apply(Bytes.wrapForRead(IOTools.readFile(filename))).getValueIn().typedMarshallable());
    }

    @NotNull
    public <T> Map<String, T> fromFileAsMap(String filename, @NotNull Class<T> tClass) throws IOException {
        @NotNull Map<String, T> map = new LinkedHashMap<>();
        Wire wire = apply(Bytes.wrapForRead(IOTools.readFile(filename)));
        @NotNull StringBuilder sb = new StringBuilder();
        while (wire.hasMore()) {
            wire.readEventName(sb)
                    .object(tClass, map, (m, o) -> m.put(sb.toString(), o));
        }
        return map;
    }

    public <T extends Marshallable> void toFileAsMap(@NotNull String filename, @NotNull Map<String, T> map) throws IOException {
        toFileAsMap(filename, map, false);
    }

    public <T extends Marshallable> void toFileAsMap(@NotNull String filename, @NotNull Map<String, T> map, boolean compact) throws IOException {
        Bytes bytes = getBytes();
        Wire wire = apply(bytes);
        for (@NotNull Map.Entry<String, T> entry : map.entrySet()) {
            @NotNull ValueOut valueOut = wire.writeEventName(entry::getKey);
            valueOut.leaf(compact).marshallable(entry.getValue());
        }
        String tempFilename = IOTools.tempName(filename);
        IOTools.writeFile(tempFilename, bytes.toByteArray());
        @NotNull File file2 = new File(tempFilename);
        @NotNull File dest = new File(filename);
        if (!file2.renameTo(dest)) {
            if (dest.delete() && file2.renameTo(dest))
                return;
            file2.delete();
            throw new IOException("Failed to rename " + tempFilename + " to " + filename);
        }
    }

    public <T> void toFile(@NotNull String filename, WriteMarshallable marshallable) throws IOException {
        Bytes bytes = getBytes();
        Wire wire = apply(bytes);
        wire.getValueOut().typedMarshallable(marshallable);
        String tempFilename = IOTools.tempName(filename);
        IOTools.writeFile(tempFilename, bytes.toByteArray());
        @NotNull File file2 = new File(tempFilename);
        if (!file2.renameTo(new File(filename))) {
            file2.delete();
            throw new IOException("Failed to rename " + tempFilename + " to " + filename);
        }
    }

    @NotNull
    String asHexString(Object marshallable) {
        Bytes bytes = asBytes(marshallable);
        return bytes.toHexString();
    }

    @Nullable
    <T> T fromHexString(@NotNull CharSequence s) {
        Wire wire = apply(Bytes.fromHexString(s.toString()));
        return wire.getValueIn().typedMarshallable();
    }

    @Nullable
    public Map<String, Object> asMap(CharSequence cs) {
        Bytes bytes = getBytes2();
        bytes.appendUtf8(cs);
        Wire wire = apply(bytes);
        return wire.getValueIn().marshallableAsMap(String.class, Object.class);
    }
}
