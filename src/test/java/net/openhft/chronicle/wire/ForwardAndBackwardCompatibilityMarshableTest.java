package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

/**
 * Created by rob on 03/05/2016.
 */

@RunWith(value = Parameterized.class)
public class ForwardAndBackwardCompatibilityMarshableTest {

    private final WireType wireType;

    public ForwardAndBackwardCompatibilityMarshableTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.TEXT},
                {WireType.BINARY}
        });
    }

    @Test
    public void marshableStringBuilderTest() throws Exception {
        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        CLASS_ALIASES.addAlias(DTO2.class, "DTO");

        wire.writeDocument(false, w -> new DTO2(1, 2, "3").writeMarshallable(w));
        System.out.println(Wires.fromSizePrefixedBlobs(wire));

        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assert.fail();
            @NotNull DTO2 dto2 = new DTO2();
            dto2.readMarshallable(dc.wire());
            Assert.assertEquals(dto2.one, 1);
            Assert.assertEquals(dto2.two, 2);
            Assert.assertTrue("3".contentEquals(dto2.three));
        }

    }

    @Test
    public void backwardsCompatibility() throws Exception {
        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        CLASS_ALIASES.addAlias(DTO1.class, "DTO");

        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new DTO1(1)));
        System.out.println(Wires.fromSizePrefixedBlobs(wire));

        CLASS_ALIASES.addAlias(DTO2.class, "DTO");

        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assert.fail();
            @NotNull DTO2 dto2 = new DTO2();
            dc.wire().getValueIn().marshallable(dto2);
            Assert.assertEquals(dto2.one, 1);
            Assert.assertEquals(dto2.two, 0);
            Assert.assertEquals(dto2.three, null);
        }

    }

    @Test
    public void forwardComparability() throws Exception {

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        CLASS_ALIASES.addAlias(DTO2.class, "DTO");

        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new DTO2(1, 2, "3")));
        System.out.println(Wires.fromSizePrefixedBlobs(wire));

        CLASS_ALIASES.addAlias(DTO1.class, "DTO");

        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assert.fail();
            @NotNull DTO1 dto1 = new DTO1();
            dc.wire().getValueIn().marshallable(dto1);
            Assert.assertEquals(dto1.one, 1);
        }

    }

    public static class DTO1 extends AbstractMarshallable implements Demarshallable {

        int one;

        @UsedViaReflection
        public DTO1(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        public DTO1(int i) {
            this.one = i;
        }

        public DTO1() {

        }

        public int one() {
            return one;
        }

        @NotNull
        public DTO1 one(int one) {
            this.one = one;
            return this;
        }
    }

    public static class DTO2 extends AbstractMarshallable implements Demarshallable {

        final StringBuilder three = new StringBuilder();
        int one;
        int two;

        @UsedViaReflection
        public DTO2(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        public DTO2(int one, int two, @NotNull Object three) {
            this.one = one;
            this.two = two;
            StringUtils.setCount(this.three, 0);
            this.three.append(three.toString());
        }

        public DTO2() {

        }

        @NotNull
        public Object three() {
            return three;
        }

        @NotNull
        public DTO2 three(@NotNull Object three) {
            StringUtils.setCount(this.three, 0);
            this.three.append(three.toString());
            return this;
        }

        public int one() {
            return one;
        }

        @NotNull
        public DTO2 one(int one) {
            this.one = one;
            return this;
        }

        public int two() {
            return two;
        }

        @NotNull
        public DTO2 two(int two) {
            this.two = two;
            return this;
        }
    }
}

