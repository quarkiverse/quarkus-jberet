package io.quarkiverse.jberet.rest.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.ejb.ScheduleExpression;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;

import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduleConfigBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class ScheduleExpressionJsonbSerializationTest {
    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    Jsonb jsonb;

    @Test
    void scheduleExpressionRoundTrip() throws Exception {
        ScheduleExpression expression = new ScheduleExpression()
                .hour("6")
                .minute("30")
                .second("0");

        JobScheduleConfig config = JobScheduleConfigBuilder.newInstance()
                .jobName("myJob")
                .scheduleExpression(expression)
                .build();

        String json = jsonb.toJson(config);

        JobScheduleConfig deserialized = jsonb.fromJson(json, JobScheduleConfig.class);

        assertNotNull(deserialized.getScheduleExpression(),
                "scheduleExpression should not be null after JSON-B deserialization");
        assertEquals("6", deserialized.getScheduleExpression().getHour(),
                "hour should be preserved after JSON-B round-trip");
        assertEquals("30", deserialized.getScheduleExpression().getMinute(),
                "minute should be preserved after JSON-B round-trip");
    }

    @Test
    void scheduleExpressionWithNumericValues() {
        String json = """
                {
                    "jobName": "myJob",
                    "scheduleExpression": { "hour": 6, "minute": 30, "second": 0 }
                }
                """;

        JobScheduleConfig deserialized = jsonb.fromJson(json, JobScheduleConfig.class);

        assertNotNull(deserialized.getScheduleExpression(),
                "scheduleExpression should not be null when using numeric values");
        assertEquals("6", deserialized.getScheduleExpression().getHour(),
                "numeric hour should be deserialized as string");
        assertEquals("30", deserialized.getScheduleExpression().getMinute(),
                "numeric minute should be deserialized as string");
    }
}
