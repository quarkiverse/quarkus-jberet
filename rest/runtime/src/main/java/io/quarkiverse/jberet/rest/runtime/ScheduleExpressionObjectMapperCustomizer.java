package io.quarkiverse.jberet.rest.runtime;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import jakarta.ejb.ScheduleExpression;
import jakarta.inject.Singleton;

import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduleConfigBuilder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class ScheduleExpressionObjectMapperCustomizer implements ObjectMapperCustomizer {
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .withZone(ZoneId.systemDefault());

    @Override
    public void customize(ObjectMapper objectMapper) {
        SimpleModule module = new SimpleModule("ScheduleExpressionModule");
        module.addSerializer(ScheduleExpression.class, new ScheduleExpressionSerializer());
        module.addDeserializer(JobScheduleConfig.class, new JobScheduleConfigDeserializer());
        objectMapper.registerModule(module);
    }

    static class ScheduleExpressionSerializer extends JsonSerializer<ScheduleExpression> {
        @Override
        public void serialize(ScheduleExpression value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            writeIfNotNull(gen, "second", value.getSecond());
            writeIfNotNull(gen, "minute", value.getMinute());
            writeIfNotNull(gen, "hour", value.getHour());
            writeIfNotNull(gen, "dayOfMonth", value.getDayOfMonth());
            writeIfNotNull(gen, "dayOfWeek", value.getDayOfWeek());
            writeIfNotNull(gen, "month", value.getMonth());
            writeIfNotNull(gen, "year", value.getYear());
            writeIfNotNull(gen, "timezone", value.getTimezone());
            if (value.getStart() != null) {
                gen.writeStringField("start", DATE_FORMATTER.format(value.getStart().toInstant()));
            }
            if (value.getEnd() != null) {
                gen.writeStringField("end", DATE_FORMATTER.format(value.getEnd().toInstant()));
            }
            gen.writeEndObject();
        }

        private void writeIfNotNull(JsonGenerator gen, String key, String value) throws IOException {
            if (value != null) {
                gen.writeStringField(key, value);
            }
        }
    }

    static class JobScheduleConfigDeserializer extends JsonDeserializer<JobScheduleConfig> {
        @Override
        public JobScheduleConfig deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectNode node = p.getCodec().readTree(p);
            JobScheduleConfigBuilder builder = JobScheduleConfigBuilder.newInstance();

            if (node.has("jobName") && !node.get("jobName").isNull()) {
                builder.jobName(node.get("jobName").asText());
            }
            if (node.has("jobExecutionId")) {
                builder.jobExecutionId(node.get("jobExecutionId").asLong());
            }
            if (node.has("jobParameters") && !node.get("jobParameters").isNull()) {
                Properties props = new Properties();
                for (Map.Entry<String, JsonNode> field : node.get("jobParameters").properties()) {
                    props.setProperty(field.getKey(), field.getValue().asText());
                }
                builder.jobParameters(props);
            }
            if (node.has("scheduleExpression") && !node.get("scheduleExpression").isNull()) {
                builder.scheduleExpression(deserializeScheduleExpression(node.get("scheduleExpression")));
            }
            if (node.has("initialDelay")) {
                builder.initialDelay(node.get("initialDelay").asLong());
            }
            if (node.has("afterDelay")) {
                builder.afterDelay(node.get("afterDelay").asLong());
            }
            if (node.has("interval")) {
                builder.interval(node.get("interval").asLong());
            }
            if (node.has("persistent")) {
                builder.persistent(node.get("persistent").asBoolean());
            }

            return builder.build();
        }

        private ScheduleExpression deserializeScheduleExpression(JsonNode node) {
            ScheduleExpression expression = new ScheduleExpression();

            setIfPresent(node, "second", expression::second);
            setIfPresent(node, "minute", expression::minute);
            setIfPresent(node, "hour", expression::hour);
            setIfPresent(node, "dayOfMonth", expression::dayOfMonth);
            setIfPresent(node, "dayOfWeek", expression::dayOfWeek);
            setIfPresent(node, "month", expression::month);
            setIfPresent(node, "year", expression::year);
            setIfPresent(node, "timezone", expression::timezone);
            if (node.has("start") && !node.get("start").isNull()) {
                expression.start(Date.from(Instant.from(DATE_FORMATTER.parse(node.get("start").asText()))));
            }
            if (node.has("end") && !node.get("end").isNull()) {
                expression.end(Date.from(Instant.from(DATE_FORMATTER.parse(node.get("end").asText()))));
            }

            return expression;
        }

        private void setIfPresent(JsonNode node, String key, java.util.function.Consumer<String> setter) {
            if (node.has(key) && !node.get(key).isNull()) {
                setter.accept(node.get(key).asText());
            }
        }
    }
}
