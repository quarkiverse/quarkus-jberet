package io.quarkiverse.jberet.runtime;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;

import org.jberet.spi.JobOperatorContext;
import org.jberet.util.BatchUtil;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public class JBeretSubstitutions {
    @TargetClass(BatchRuntime.class)
    static final class Target_BatchRuntime {
        @Substitute
        public static JobOperator getJobOperator() {
            return JobOperatorContext.getJobOperatorContext().getJobOperator();
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

    /**
     * JBeret Java-serializes step exceptions and checkpoint data. In native mode, serializing a class not registered
     * for serialization throws an {@link Error} instead of an {@link IOException}, which bypasses the JBeret fallback
     * in {@link org.jberet.runtime.SerializableData} and breaks the step failure handling.
     */
    @TargetClass(className = "org.jberet.spi.SerializableDataProvider$DefaultSerializableDataProvider")
    static final class Target_DefaultSerializableDataProvider {
        @Substitute
        public byte[] objectToBytes(final Object obj) throws IOException {
            try {
                return BatchUtil.objectToBytes(obj);
            } catch (IOException | VirtualMachineError e) {
                throw e;
            } catch (Throwable e) {
                if (obj instanceof Throwable throwable) {
                    return BatchUtil.objectToBytes(
                            toSerializableException(throwable, Collections.newSetFromMap(new IdentityHashMap<>())));
                }
                throw new IOException(e);
            }
        }
    }

    /**
     * Replaces a {@link Throwable} and its causes with {@link BatchRuntimeException}, which is registered for
     * serialization, keeping the original type and message (via {@link Throwable#toString()}) and the stack trace.
     */
    static BatchRuntimeException toSerializableException(final Throwable throwable, final Set<Throwable> visited) {
        visited.add(throwable);
        Throwable cause = throwable.getCause();
        BatchRuntimeException exception = new BatchRuntimeException(throwable.toString(),
                cause != null && !visited.contains(cause) ? toSerializableException(cause, visited) : null);
        exception.setStackTrace(throwable.getStackTrace());
        return exception;
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
