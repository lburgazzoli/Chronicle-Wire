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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Created by peter on 24/03/16.
 */
public class MethodReader implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodReader.class);
    private final MarshallableIn in;
    @NotNull
    private final WireParser<Void> wireParser;
    private boolean closeIn = false, closed;

    public MethodReader(MarshallableIn in, @NotNull Object... objects) {
        this.in = in;
        @NotNull WireParselet defaultParselet = (s, v, $) ->
                LOGGER.warn("Unknown message " + s + ' ' + v.text());
        if (objects[0] instanceof WireParselet)
            defaultParselet = (WireParselet) objects[0];
        wireParser = WireParser.wireParser(defaultParselet);

        @NotNull Set<String> methodsHandled = new HashSet<>();
        for (@NotNull Object o : objects) {
            for (@NotNull Method m : o.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers()))
                    continue;

                try {
                    Object.class.getMethod(m.getName(), m.getParameterTypes());
                    continue;
                } catch (NoSuchMethodException e) {
                    // not an Object method.
                }

                if (!methodsHandled.add(m.getName()))
                    continue;

                Class<?>[] parameterTypes = m.getParameterTypes();
                switch (parameterTypes.length) {
                    case 0:

                        break;
                    case 1:
                        addParseletForMethod(o, m, parameterTypes[0]);
                        break;
                    default:
                        addParseletForMethod(o, m, parameterTypes);
                        break;
                }
            }
        }
        if (wireParser.lookup("history") == null) {
            wireParser.register(() -> "history", (s, v, $) -> {
                v.marshallable(MessageHistory.get());
            });
        }
    }

    static void logMessage(@NotNull CharSequence s, @NotNull ValueIn v) {
        @NotNull String name = s.toString();
        String rest;

        if (v.wireIn() instanceof BinaryWire) {
            Bytes bytes = Bytes.elasticByteBuffer((int) (v.wireIn().bytes().readRemaining() * 3 / 2 + 64));
            long pos = v.wireIn().bytes().readPosition();
            v.wireIn().copyTo(new TextWire(bytes));
            v.wireIn().bytes().readPosition(pos);
            rest = bytes.toString();
        } else {
            rest = v.toString();
        }
        LOGGER.debug("read " + name + " - " + rest);
    }

    @NotNull
    public MethodReader closeIn(boolean closeIn) {
        this.closeIn = closeIn;
        return this;
    }

    public void addParseletForMethod(Object o, @NotNull Method m, Class<?> parameterType) {
        Class msgClass = parameterType;
        m.setAccessible(true); // turn of security check to make a little faster
        if (msgClass.isInterface() || !ReadMarshallable.class.isAssignableFrom(msgClass)) {
            @NotNull Object[] argArr = {null};
            wireParser.register(m::getName, (s, v, $) -> {
                try {
                    if (Jvm.isDebug())
                        logMessage(s, v);

                    argArr[0] = v.object(msgClass);
                    m.invoke(o, argArr);
                } catch (Exception i) {
                    LOGGER.warn("Failure to dispatch message: " + m.getName() + " " + argArr[0], i);
                }
            });

        } else {
            ReadMarshallable arg;
            try {
                arg = (ReadMarshallable) msgClass.newInstance();
            } catch (Exception e) {
                try {
                    arg = (ReadMarshallable) OS.memory().allocateInstance(msgClass);
                } catch (InstantiationException e1) {
                    throw Jvm.rethrow(e1);
                }
            }
            @NotNull ReadMarshallable[] argArr = {arg};
            wireParser.register(m::getName, (s, v, $) -> {
                try {
                    if (Jvm.isDebug())
                        logMessage(s, v);

                    v.marshallable(argArr[0]);
                    m.invoke(o, argArr);
                } catch (Exception i) {
                    LOGGER.warn("Failure to dispatch message: " + m.getName() + " " + argArr[0], i);
                }
            });
        }
    }

    public void addParseletForMethod(Object o, @NotNull Method m, @NotNull Class[] parameterTypes) {
        m.setAccessible(true); // turn of security check to make a little faster
        @NotNull Object[] args = new Object[parameterTypes.length];
        @NotNull BiConsumer<Object[], ValueIn> sequenceReader = (a, v) -> {
            int i = 0;
            for (@NotNull Class clazz : parameterTypes) {
                a[i++] = v.object(clazz);
            }
        };
        wireParser.register(m::getName, (s, v, $) -> {
            try {
                if (Jvm.isDebug())
                    logMessage(s, v);

                v.sequence(args, sequenceReader);
                m.invoke(o, args);
            } catch (Exception i) {
                LOGGER.warn("Failure to dispatch message: " + m.getName() + " " + Arrays.toString(args), i);
            }
        });
    }

    /**
     * reads one message
     *
     * @return true if there was a message, or false if not.
     */
    public boolean readOne() {
        MessageHistory.get().reset();
        try (DocumentContext context = in.readingDocument()) {
            if (!context.isData())
                return false;
            wireParser.accept(context.wire(), null);
        }
        return true;
    }

    @Override
    public void close() {
        if (closeIn)
            Closeable.closeQuietly(in);
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
