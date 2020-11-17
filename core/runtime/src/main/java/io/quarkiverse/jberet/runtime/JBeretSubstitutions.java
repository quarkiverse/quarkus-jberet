package io.quarkiverse.jberet.runtime;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

import javax.batch.operations.BatchRuntimeException;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;

import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;
import org.jberet.runtime.SerializableData;
import org.jberet.util.BatchUtil;

import com.oracle.svm.core.SubstrateUtil;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

/**
 * Used to replace Java Serialization not supported in SVM by Jsonb Serialization.
 *
 * This is just a way to make JBeret work in native mode. Since we only support the in memory repository for now, this
 * should be fine. If we add support to a persistent repository, then the data written by the JVM version and the SVM
 * version will not be compatible.
 */
public class JBeretSubstitutions {
    static final Jsonb jsonb = JsonbBuilder.newBuilder().build();

    @TargetClass(SerializableData.class)
    static final class Target_SerializableData {
        @Alias
        private byte[] serialized;
        @Alias
        private Serializable raw;

        @Alias
        @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
        Target_SerializableData(final byte[] serialized, final Serializable raw) {

        }

        @Substitute
        public static SerializableData of(final Serializable data) {
            if (data instanceof SerializableData) {
                return (SerializableData) data;
            }
            if (data instanceof byte[]) {
                return SubstrateUtil.cast(new Target_SerializableData((byte[]) data, null), SerializableData.class);
            }
            if (data == null) {
                return SubstrateUtil.cast(new Target_SerializableData(null, null), SerializableData.class);
            }

            Class<?> c = data.getClass();
            if (c.isArray()) {
                c = c.getComponentType();
            }

            if (requiresSerialization(c)) {
                try {
                    return SubstrateUtil.cast(new Target_SerializableData(jsonb.toJson(data).getBytes(), data),
                            SerializableData.class);
                } catch (JsonbException e) {
                    if (data instanceof Throwable) {
                        //if failed to serialize step exception data, try to preserve original
                        //step exception message and stack trace
                        final Throwable exceptionData = (Throwable) data;
                        BatchLogger.LOGGER.failedToSerializeException(e, exceptionData);
                        final BatchRuntimeException replacementException = new BatchRuntimeException(
                                exceptionData.getMessage());
                        replacementException.setStackTrace(exceptionData.getStackTrace());
                        try {
                            return SubstrateUtil.cast(new Target_SerializableData(jsonb.toJson(data).getBytes(), data),
                                    SerializableData.class);
                        } catch (final JsonbException e1) {
                            throw BatchMessages.MESSAGES.failedToSerialize(e1, replacementException);
                        }
                    }
                    throw BatchMessages.MESSAGES.failedToSerialize(e, data);
                }
            }
            return SubstrateUtil.cast(new Target_SerializableData(null, data), SerializableData.class);
        }

        @Substitute
        public Serializable deserialize() {
            if (serialized != null) {
                try {
                    return jsonb.fromJson(new String(serialized), raw.getClass());
                } catch (JsonbException e) {
                    throw BatchMessages.MESSAGES.failedToDeserialize(e, Arrays.toString(serialized));
                }
            }

            if (raw != null) {
                return raw;
            }

            return null;
        }

        @Substitute
        byte[] getSerialized() throws RuntimeException {
            if (serialized != null) {
                return serialized;
            }
            try {
                return jsonb.toJson(raw).getBytes();
            } catch (final JsonbException e) {
                throw BatchMessages.MESSAGES.failedToSerialize(e, raw);
            }
        }

        @Alias
        private static boolean requiresSerialization(final Class<?> c) {
            return false;
        }
    }

    @TargetClass(className = "org.jberet.repository.TableColumns")
    static final class Target_TableColumns {
        @Alias
        static int EXECUTION_EXCEPTION_LENGTH_LIMIT = 0;

        @Substitute
        static String formatException(final Exception exception) {
            if (exception == null) {
                return null;
            }

            String asString = getStackTraceAsString(exception);
            final Charset charset = Charset.defaultCharset();
            byte[] asBytes = asString.getBytes(charset);
            if (asBytes.length <= EXECUTION_EXCEPTION_LENGTH_LIMIT) {
                return asString;
            }

            asString = exception + BatchUtil.NL + getRootCause(exception);
            asBytes = asString.getBytes(charset);
            if (asBytes.length <= EXECUTION_EXCEPTION_LENGTH_LIMIT) {
                return asString;
            }

            final ByteBuffer bb = ByteBuffer.wrap(asBytes, 0, EXECUTION_EXCEPTION_LENGTH_LIMIT);
            final CharBuffer cb = CharBuffer.allocate(EXECUTION_EXCEPTION_LENGTH_LIMIT);
            final CharsetDecoder decoder = charset.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.IGNORE);
            decoder.decode(bb, cb, true);
            decoder.flush(cb);
            return new String(cb.array(), 0, cb.position());
        }
    }

    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public static Throwable getRootCause(Throwable throwable) {
        // Keep a second pointer that slowly walks the causal chain. If the fast pointer ever catches
        // the slower pointer, then there's a loop.
        Throwable slowPointer = throwable;
        boolean advanceSlowPointer = false;

        Throwable cause;
        while ((cause = throwable.getCause()) != null) {
            throwable = cause;

            if (throwable == slowPointer) {
                throw new IllegalArgumentException("Loop in causal chain detected.", throwable);
            }
            if (advanceSlowPointer) {
                slowPointer = slowPointer.getCause();
            }
            advanceSlowPointer = !advanceSlowPointer; // only advance every other iteration
        }
        return throwable;
    }
}
