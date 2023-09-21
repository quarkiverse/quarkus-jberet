package io.quarkiverse.jberet.deployment;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.runtime.BatchStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class JobProducerInXmlTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DummyBatchlet.class)
                    .addAsManifestResource("batchlet.xml", "batch-jobs/job.xml"))
            .setExpectedException(AmbiguousResolutionException.class);

    @Test
    void validate() {
    }

    @Named("batchlet")
    @Dependent
    public static class DummyBatchlet implements Batchlet {

        @Inject
        @BatchProperty(name = "name")
        String name;

        @Override
        public String process() {
            if (!name.equals("david")) {
                throw new RuntimeException("Unexpected value injected to 'name': " + name);
            }
            return BatchStatus.COMPLETED.toString();
        }

        @Override
        public void stop() {
        }
    }

    @ApplicationScoped
    public static class JobProducer {
        @Produces
        @Named
        public Job job() {
            return new JobBuilder("job")
                    .step(new StepBuilder("step").batchlet("batchlet", new String[] { "name", "#{jobParameters['name']}" })
                            .build())
                    .build();
        }
    }
}
