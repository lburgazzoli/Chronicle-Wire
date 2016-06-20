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

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import static net.openhft.chronicle.wire.WireType.READ_ANY;
import static net.openhft.chronicle.wire.WireType.TEXT;

/**
 * The implementation of this interface is both readable and write-able as marshallable data.
 */
public interface Marshallable extends WriteMarshallable, ReadMarshallable {
    static boolean $equals(@NotNull WriteMarshallable $this, Object o) {
        return o instanceof WriteMarshallable &&
                ($this == o || Wires.isEquals($this, o));
    }

    static int $hashCode(WriteMarshallable $this) {
        return HashWire.hash32($this);
    }

    static String $toString(WriteMarshallable $this) {
        return TEXT.asString($this);
    }

    @Nullable
    static <T> T fromString(CharSequence cs) {
        return TEXT.fromString(cs);
    }

    @Nullable
    static <T> T fromFile(String filename) throws IOException {
        return TEXT.fromFile(filename);
    }

    @NotNull
    static Map<String, Object> fromFileAsMap(String filename) throws IOException {
        return TEXT.fromFileAsMap(filename, Object.class);
    }

    @NotNull
    static <V> Map<String, V> fromFileAsMap(String filename, @NotNull Class<V> valueClass) throws IOException {
        return TEXT.fromFileAsMap(filename, valueClass);
    }

    @Nullable
    static Map<String, Object> fromHexString(@NotNull CharSequence cs) {
        return READ_ANY.fromHexString(cs);
    }

    @Override
    default void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        Wires.readMarshallable(this, wire, true);
    }

    @Override
    default void writeMarshallable(@NotNull WireOut wire) {
        Wires.writeMarshallable(this, wire);
    }

    @NotNull
    default <T extends Marshallable & KeyedMarshallable> T deepCopy() {
        return (T) Wires.deepCopy(this);
    }

    default <T extends Marshallable> T copyFrom(T t) {
        return Wires.copyTo(this, t);
    }

    default <K, T extends Marshallable> T mergeToMap(@NotNull Map<K, T> map, @NotNull Function<T, K> getKey) {
        @NotNull @SuppressWarnings("unchecked")
        T t = (T) this;
        return map.merge(getKey.apply(t), t,
                (p, c) -> p == null ? c.deepCopy() : p.copyFrom(c));
    }

  /*  static <T extends Marshallable & KeyedMarshallable> T deepCopy(T t) {
        return (T) Wires.deepCopy(t);
    }*/
}
