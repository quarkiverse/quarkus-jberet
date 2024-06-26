package io.quarkiverse.jberet.deployment;

import static org.awaitility.Awaitility.await;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.spi.JobXmlResolver;
import org.jberet.tools.AbstractJobXmlResolver;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CustomJobXmlResolverTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsServiceProvider(JobXmlResolver.class, CustomJobXmlResolver.class));

    @Inject
    JobOperator jobOperator;

    @Test
    public void runBatchletJob() {
        Properties jobParameters = new Properties();
        jobParameters.setProperty("name", "david");
        long executionId = jobOperator.start("batchlet", jobParameters);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);
            return BatchStatus.COMPLETED.equals(jobExecution.getBatchStatus());
        });
    }

    @Named("batchlet")
    @Dependent
    public static class MyBatchlet implements Batchlet {

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

    public static class CustomJobXmlResolver extends AbstractJobXmlResolver implements JobXmlResolver {
        @Override
        public InputStream resolveJobXml(final String jobXml, final ClassLoader classLoader) {
            String job = "<job id=\"batchlet-job\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" version=\"1.0\">\n" +
                    "    <step id=\"batchlet-step\">\n" +
                    "        <batchlet ref=\"batchlet\">\n" +
                    "            <properties>\n" +
                    "                <property name=\"name\" value=\"#{jobParameters['name']}\"/>\n" +
                    "            </properties>\n" +
                    "        </batchlet>\n" +
                    "    </step> \n" +
                    "</job>\n";

            return new ByteArrayInputStream(job.getBytes());
        }

        @Override
        public String resolveJobName(final String jobXml, final ClassLoader classLoader) {
            return super.resolveJobName(jobXml, classLoader);
        }

        @Override
        public Collection<String> getJobXmlNames(final ClassLoader classLoader) {
            return Set.of("batchlet");
        }
    }
}
