package io.quarkiverse.jberet.rest.runtime;

import static io.quarkiverse.jberet.runtime.JBeretSubstitutions.getStackTraceAsString;

import java.io.Serializable;

import jakarta.batch.operations.BatchRuntimeException;

import org.jberet.rest.entity.BatchExceptionEntity;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

public class JBeretRestSubstitutions {
    @TargetClass(BatchExceptionEntity.class)
    static final class Target_BatchExceptionEntity implements Serializable {
        @Alias
        private Class<? extends BatchRuntimeException> type;
        @Alias
        private String message;
        @Alias
        private String stackTrace;

        @Substitute
        @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
        public Target_BatchExceptionEntity(final BatchRuntimeException ex) {
            type = ex.getClass();
            message = ex.getMessage();
            stackTrace = getStackTraceAsString(ex);
        }
    }
}
