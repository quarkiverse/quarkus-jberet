package io.quarkiverse.jberet.rest.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.ejb.ScheduleExpression;

import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduleConfigBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.jberet.rest.runtime.ScheduleExpressionObjectMapperCustomizer;

public class ScheduleExpressionJacksonTest {
    static ObjectMapper objectMapper;

    @BeforeAll
    static void setUp() {
        objectMapper = new ObjectMapper();
        new ScheduleExpressionObjectMapperCustomizer().customize(objectMapper);
    }

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

        String json = objectMapper.writeValueAsString(config);

        JobScheduleConfig deserialized = objectMapper.readValue(json, JobScheduleConfig.class);

        assertNotNull(deserialized.getScheduleExpression(),
                "scheduleExpression should not be null after JSON deserialization");
        assertEquals("6", deserialized.getScheduleExpression().getHour(),
                "hour should be preserved after JSON round-trip");
        assertEquals("30", deserialized.getScheduleExpression().getMinute(),
                "minute should be preserved after JSON round-trip");
    }

    @Test
    void scheduleExpressionWithNumericValues() throws Exception {
        String json = """
                {
                    "jobName": "myJob",
                    "scheduleExpression": { "hour": 6, "minute": 30, "second": 0 }
                }
                """;

        JobScheduleConfig deserialized = objectMapper.readValue(json, JobScheduleConfig.class);

        assertNotNull(deserialized.getScheduleExpression(),
                "scheduleExpression should not be null when using numeric values");
        assertEquals("6", deserialized.getScheduleExpression().getHour(),
                "numeric hour should be deserialized as string");
        assertEquals("30", deserialized.getScheduleExpression().getMinute(),
                "numeric minute should be deserialized as string");
    }
}
