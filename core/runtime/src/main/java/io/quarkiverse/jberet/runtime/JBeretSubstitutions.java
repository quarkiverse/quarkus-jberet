package io.quarkiverse.jberet.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;

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
