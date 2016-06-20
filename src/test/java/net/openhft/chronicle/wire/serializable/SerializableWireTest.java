package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 09/05/16.
 */
@RunWith(value = Parameterized.class)
public class SerializableWireTest {
    private final WireType wireType;
    private final Serializable m;

    public SerializableWireTest(WireType wireType, Serializable m) {
        this.wireType = wireType;
        this.m = m;
    }

    @NotNull
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        @NotNull WireType[] wireTypes = {WireType.TEXT/*, WireType.BINARY*/};
        @NotNull Serializable[] objects = {
                new Nested(),
                new Nested(new ScalarValues(), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap()),
                new ScalarValues(),
                new ScalarValues(1),
                new ScalarValues(10)
        };
        for (WireType wt : wireTypes) {
            for (Serializable object : objects) {
                @NotNull Object[] test = {wt, object};
                list.add(test);
            }
        }
        return list;
    }

    @Test
    public void writeMarshallable() {
        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);

        wire.getValueOut().object(m);
        System.out.println(wire);

        @Nullable Object m2 = wire.getValueIn()
                .object();
        assertEquals(m, m2);
    }
}
