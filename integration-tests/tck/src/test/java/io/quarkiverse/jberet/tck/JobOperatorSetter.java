package io.quarkiverse.jberet.tck;

import java.lang.reflect.Field;

import jakarta.batch.operations.JobOperator;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;

public class JobOperatorSetter {
    static void setJobOperator(Object instance, JobOperator jobOperator) throws Exception {
        final Field jobOp = instance.getClass().getSuperclass().getDeclaredField("jobOp");
        jobOp.setAccessible(true);

        final JobOperatorBridge bridge = (JobOperatorBridge) jobOp.get(instance);
        final Field jobOpBridge = bridge.getClass().getDeclaredField("jobOp");
        jobOpBridge.setAccessible(true);
        jobOpBridge.set(bridge, jobOperator);
    }
}
