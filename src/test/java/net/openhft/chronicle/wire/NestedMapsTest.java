package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static java.util.Collections.addAll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by peter on 21/04/16.
 */
@RunWith(value = Parameterized.class)
public class NestedMapsTest {
    private final WireType wireType;

    public NestedMapsTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.TEXT},
                new Object[]{WireType.BINARY},
                new Object[]{WireType.FIELDLESS_BINARY}
        );
    }

    @Test
    public void testMapped() {
        @NotNull Mapped m = new Mapped();
        addAll(m.words, "A quick brown fox jumps over the lazy dog".split(" "));
        addAll(m.numbers, 1, 2, 2, 3, 5, 8, 13);
        m.map1.put("aye", "AAA");
        m.map1.put("bee", "BBB");
        m.map2.put("one", 1.0);
        m.map2.put("two point two", 2.2);

        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);
        wire.writeDocument(false, w -> w.writeEventName("mapped").object(m));
        switch (wireType) {
            case TEXT:
                assertEquals("--- !!data\n" +
                        "mapped: !net.openhft.chronicle.wire.NestedMapsTest$Mapped {\n" +
                        "  words: [\n" +
                        "    A,\n" +
                        "    quick,\n" +
                        "    brown,\n" +
                        "    fox,\n" +
                        "    jumps,\n" +
                        "    over,\n" +
                        "    the,\n" +
                        "    lazy,\n" +
                        "    dog\n" +
                        "  ]\n" +
                        "  numbers: [\n" +
                        "    1, 2, 2, 3, 5, 8, 13\n" +
                        "  ]\n" +
                        "  map1: {\n" +
                        "    aye: AAA, bee: BBB\n" +
                        "  },\n" +
                        "  map2: {\n" +
                        "    one: 1.0, two point two: 2.2\n" +
                        "  }\n" +
                        "}\n", Wires.fromSizePrefixedBlobs(wire));
                break;
            case BINARY:
                assertEquals("--- !!data #binary\n" +
                        "mapped: !net.openhft.chronicle.wire.NestedMapsTest$Mapped {\n" +
                        "  words: [\n" +
                        "    A,\n" +
                        "    quick,\n" +
                        "    brown,\n" +
                        "    fox,\n" +
                        "    jumps,\n" +
                        "    over,\n" +
                        "    the,\n" +
                        "    lazy,\n" +
                        "    dog\n" +
                        "  ]\n" +
                        "  numbers: [\n" +
                        "    1,\n" +
                        "    2,\n" +
                        "    2,\n" +
                        "    3,\n" +
                        "    5,\n" +
                        "    8,\n" +
                        "    13\n" +
                        "  ]\n" +
                        "  map1: [\n" +
                        "    aye: AAA,\n" +
                        "    bee: BBB\n" +
                        "  ]\n" +
                        "  map2: [\n" +
                        "    one: 1.0,\n" +
                        "    two point two: 2.2\n" +
                        "  ]\n" +
                        "}\n", Wires.fromSizePrefixedBlobs(wire));
                break;
            case FIELDLESS_BINARY:
                assertEquals("--- !!data #binary\n" +
                        "mapped: !net.openhft.chronicle.wire.NestedMapsTest$Mapped [[\n" +
                        "    A,\n" +
                        "    quick,\n" +
                        "    brown,\n" +
                        "    fox,\n" +
                        "    jumps,\n" +
                        "    over,\n" +
                        "    the,\n" +
                        "    lazy,\n" +
                        "    dog\n" +
                        "  ][\n" +
                        "    1,\n" +
                        "    2,\n" +
                        "    2,\n" +
                        "    3,\n" +
                        "    5,\n" +
                        "    8,\n" +
                        "    13\n" +
                        "  ][\n" +
                        "    aye: AAA,\n" +
                        "    bee: BBB\n" +
                        "  ][\n" +
                        "    one: 1.0,\n" +
                        "    two point two: 2.2\n" +
                        "  ]\n" +
                        "]\n", Wires.fromSizePrefixedBlobs(wire));
                break;
        }
        @NotNull Mapped m2 = new Mapped();
        assertTrue(wire.readDocument(null, w -> w.read(() -> "mapped").marshallable(m2)));
        assertEquals(m, m2);
    }

    @Test
    public void testMappedTopLevel() {
        @NotNull Mapped m = new Mapped();
        addAll(m.words, "A quick brown fox jumps over the lazy dog".split(" "));
        addAll(m.numbers, 1, 2, 2, 3, 5, 8, 13);
        m.map1.put("aye", "AAA");
        m.map1.put("bee", "BBB");
        m.map2.put("one", 1.0);
        m.map2.put("two point two", 2.2);

        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);
        m.writeMarshallable(wire);
        switch (wireType) {
            case TEXT:
                assertEquals("words: [\n" +
                        "  A,\n" +
                        "  quick,\n" +
                        "  brown,\n" +
                        "  fox,\n" +
                        "  jumps,\n" +
                        "  over,\n" +
                        "  the,\n" +
                        "  lazy,\n" +
                        "  dog\n" +
                        "]\n" +
                        "numbers: [\n" +
                        "  1, 2, 2, 3, 5, 8, 13\n" +
                        "]\n" +
                        "map1: {\n" +
                        "  aye: AAA, bee: BBB\n" +
                        "}\n" +
                        "map2: {\n" +
                        "  one: 1.0, two point two: 2.2\n" +
                        "}\n", wire.toString());
                break;
            case BINARY:
                assertEquals("[pos: 0, rlim: 149, wlim: 8EiB, cap: 8EiB ] ‖" +
                        "Åwords\\u0082*٠٠٠áAåquickåbrownãfoxåjumpsäoverãtheälazyãdog" +
                        "Çnumbers\\u0082⒎٠٠٠⒈⒉⒉⒊⒌⒏⒔" +
                        "Ämap1\\u0082⒙٠٠٠¹⒊ayeãAAA¹⒊beeãBBB" +
                        "Ämap2\\u0082&٠٠٠¹⒊one\\u0091٠٠٠٠٠٠ð?¹⒔two point two\\u0091\\u009A\\u0099\\u0099\\u0099\\u0099\\u0099⒈@‡" +
                        "٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());
                break;
            case FIELDLESS_BINARY:
                assertEquals("[pos: 0, rlim: 125, wlim: 8EiB, cap: 8EiB ] ‖" +
                        "\\u0082*٠٠٠áAåquickåbrownãfoxåjumpsäoverãtheälazyãdog" +
                        "\\u0082⒎٠٠٠⒈⒉⒉⒊⒌⒏⒔\\u0082⒙٠٠٠¹⒊ayeãAAA¹⒊beeãBBB" +
                        "\\u0082&٠٠٠¹⒊one\\u0091٠٠٠٠٠٠ð?¹⒔two point two\\u0091\\u009A\\u0099\\u0099\\u0099\\u0099\\u0099⒈@" +
                        "‡٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());
                break;
        }
        @NotNull Mapped m2 = new Mapped();
        m2.readMarshallable(wire);
        assertEquals(m, m2);
    }

    static class Mapped extends AbstractMarshallable {
        final Set<String> words = new LinkedHashSet<>();
        final List<Integer> numbers = new ArrayList<>();
        final Map<String, String> map1 = new LinkedHashMap<>();
        final Map<String, Double> map2 = new LinkedHashMap<>();
    }
}
