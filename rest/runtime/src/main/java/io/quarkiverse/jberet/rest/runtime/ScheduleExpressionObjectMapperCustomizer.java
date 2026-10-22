package io.quarkiverse.jberet.rest.runtime;

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

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Singleton
public class ScheduleExpressionObjectMapperCustomizer implements JsonMapperBuilderCustomizer {
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .withZone(ZoneId.systemDefault());

    @Override
    public void customize(JsonMapper.Builder builder) {
        SimpleModule module = new SimpleModule("ScheduleExpressionModule");
        module.addSerializer(ScheduleExpression.class, new ScheduleExpressionSerializer());
        module.addDeserializer(JobScheduleConfig.class, new JobScheduleConfigDeserializer());
        builder.addModule(module);
    }

    static class ScheduleExpressionSerializer extends ValueSerializer<ScheduleExpression> {
        @Override
        public void serialize(ScheduleExpression value, JsonGenerator gen, SerializationContext ctxt) {
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
                gen.writeStringProperty("start", DATE_FORMATTER.format(value.getStart().toInstant()));
            }
            if (value.getEnd() != null) {
                gen.writeStringProperty("end", DATE_FORMATTER.format(value.getEnd().toInstant()));
            }
            gen.writeEndObject();
        }

        private void writeIfNotNull(JsonGenerator gen, String key, String value) {
            if (value != null) {
                gen.writeStringProperty(key, value);
            }
        }
    }

    static class JobScheduleConfigDeserializer extends ValueDeserializer<JobScheduleConfig> {
        @Override
        public JobScheduleConfig deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode node = ctxt.readTree(p);
            JobScheduleConfigBuilder builder = JobScheduleConfigBuilder.newInstance();

            if (node.has("jobName") && !node.get("jobName").isNull()) {
                builder.jobName(node.get("jobName").asString());
            }
            if (node.has("jobExecutionId")) {
                builder.jobExecutionId(node.get("jobExecutionId").asLong());
            }
            if (node.has("jobParameters") && !node.get("jobParameters").isNull()) {
                Properties props = new Properties();
                for (Map.Entry<String, JsonNode> field : node.get("jobParameters").properties()) {
                    props.setProperty(field.getKey(), field.getValue().asString());
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
                expression.start(Date.from(Instant.from(DATE_FORMATTER.parse(node.get("start").asString()))));
            }
            if (node.has("end") && !node.get("end").isNull()) {
                expression.end(Date.from(Instant.from(DATE_FORMATTER.parse(node.get("end").asString()))));
            }

            return expression;
        }

        private void setIfPresent(JsonNode node, String key, java.util.function.Consumer<String> setter) {
            if (node.has(key) && !node.get(key).isNull()) {
                setter.accept(node.get(key).asString());
            }
        }
    }
}
