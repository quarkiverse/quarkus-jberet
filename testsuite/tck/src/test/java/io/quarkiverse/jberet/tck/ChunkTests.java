package io.quarkiverse.jberet.tck;

import static io.quarkiverse.jberet.tck.JobOperatorSetter.setJobOperator;

import javax.batch.operations.JobOperator;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ChunkTests extends com.ibm.jbatch.tck.tests.jslxml.ChunkTests {
    @Inject
    JobOperator jobOperator;

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
        setJobOperator(this, jobOperator);
    }

    @Test
    public void testChunkNoProcessorDefined() throws Exception {
        super.testChunkNoProcessorDefined();
    }

    @Test
    public void testChunkNullCheckpointInfo() throws Exception {
        super.testChunkNullCheckpointInfo();
    }

    @Test
    public void testChunkArtifactInstanceUniqueness() throws Exception {
        super.testChunkArtifactInstanceUniqueness();
    }

    @Test
    public void testChunkOnErrorListener() throws Exception {
        super.testChunkOnErrorListener();
    }

    @Test
    public void testChunkRestartItemCount7() throws Exception {
        super.testChunkRestartItemCount7();
    }

    @Test
    public void testChunkRestartItemCount10() throws Exception {
        super.testChunkRestartItemCount10();
    }

    @Test
    public void testChunkRestartChunk5() throws Exception {
        super.testChunkRestartChunk5();
    }

    @Test
    public void testChunkDefaultItemCount() throws Exception {
        super.testChunkDefaultItemCount();
    }

    @Test
    public void testChunkRestartCustomCheckpoint() throws Exception {
        super.testChunkRestartCustomCheckpoint();
    }

    @Test
    public void testChunkTimeBasedDefaultCheckpoint() throws Exception {
        super.testChunkTimeBasedDefaultCheckpoint();
    }

    @Test
    public void testChunkTimeBasedTimeLimit0() throws Exception {
        super.testChunkTimeBasedTimeLimit0();
    }

    @Test
    public void testChunkTimeBased10Seconds() throws Exception {
        super.testChunkTimeBased10Seconds();
    }

    @Test
    public void testChunkRestartTimeBasedCheckpoint() throws Exception {
        super.testChunkRestartTimeBasedCheckpoint();
    }

    @Test
    public void testChunkRestartTimeBasedDefaultCheckpoint() throws Exception {
        super.testChunkRestartTimeBasedDefaultCheckpoint();
    }

    @Test
    @Disabled
    public void testChunkSkipMultipleExceptions() throws Exception {
        super.testChunkSkipMultipleExceptions();
    }

    @Test
    public void testChunkSkipRead() throws Exception {
        super.testChunkSkipRead();
    }

    @Test
    public void testChunkSkipProcess() throws Exception {
        super.testChunkSkipProcess();
    }

    @Test
    public void testChunkSkipWrite() throws Exception {
        super.testChunkSkipWrite();
    }

    @Test
    public void testChunkSkipOnError() throws Exception {
        super.testChunkSkipOnError();
    }

    @Test
    public void testChunkRetryOnError() throws Exception {
        super.testChunkRetryOnError();
    }

    @Test
    public void testChunkSkipReadExceedSkip() throws Exception {
        super.testChunkSkipReadExceedSkip();
    }

    @Test
    public void testChunkSkipProcessExceedSkip() throws Exception {
        super.testChunkSkipProcessExceedSkip();
    }

    @Test
    public void testChunkSkipWriteExceedSkip() throws Exception {
        super.testChunkSkipWriteExceedSkip();
    }

    @Test
    public void testChunkSkipReadNoSkipChildEx() throws Exception {
        super.testChunkSkipReadNoSkipChildEx();
    }

    @Test
    public void testChunkRetryRead() throws Exception {
        super.testChunkRetryRead();
    }

    @Test
    public void testChunkRetryMultipleExceptions() throws Exception {
        super.testChunkRetryMultipleExceptions();
    }

    @Test
    public void testChunkItemListeners() throws Exception {
        super.testChunkItemListeners();
    }

    @Test
    public void testChunkItemListenersOnError() throws Exception {
        super.testChunkItemListenersOnError();
    }

    @Test
    public void testUserDataIsPersistedAfterCheckpoint() throws Exception {
        super.testUserDataIsPersistedAfterCheckpoint();
    }
}
