package io.quarkiverse.jberet.rest.runtime;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Properties;
import java.util.function.Consumer;

import jakarta.ejb.ScheduleExpression;
import jakarta.inject.Singleton;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

import org.jberet.schedule.JobScheduleConfig;
import org.jberet.schedule.JobScheduleConfigBuilder;

import io.quarkus.jsonb.JsonbConfigCustomizer;

@Singleton
public class ScheduleExpressionJsonbConfigCustomizer implements JsonbConfigCustomizer {
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .withZone(ZoneId.systemDefault());

    @Override
    public void customize(JsonbConfig config) {
        config.withSerializers(new ScheduleExpressionSerializer());
        config.withDeserializers(new JobScheduleConfigDeserializer());
    }

    static class ScheduleExpressionSerializer implements JsonbSerializer<ScheduleExpression> {
        @Override
        public void serialize(ScheduleExpression value, JsonGenerator gen, SerializationContext ctx) {
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
                gen.write("start", DATE_FORMATTER.format(value.getStart().toInstant()));
            }
            if (value.getEnd() != null) {
                gen.write("end", DATE_FORMATTER.format(value.getEnd().toInstant()));
            }
            gen.writeEnd();
        }

        private void writeIfNotNull(JsonGenerator gen, String key, String value) {
            if (value != null) {
                gen.write(key, value);
            }
        }
    }

    static class JobScheduleConfigDeserializer implements JsonbDeserializer<JobScheduleConfig> {
        @Override
        public JobScheduleConfig deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            JsonObject json = parser.getObject();
            JobScheduleConfigBuilder builder = JobScheduleConfigBuilder.newInstance();

            if (json.containsKey("jobName") && !json.isNull("jobName")) {
                builder.jobName(json.getString("jobName"));
            }
            if (json.containsKey("jobExecutionId")) {
                builder.jobExecutionId(getLong(json, "jobExecutionId"));
            }
            if (json.containsKey("jobParameters") && !json.isNull("jobParameters")) {
                Properties props = new Properties();
                JsonObject paramsObj = json.getJsonObject("jobParameters");
                for (String key : paramsObj.keySet()) {
                    props.setProperty(key, getAsString(paramsObj.get(key)));
                }
                builder.jobParameters(props);
            }
            if (json.containsKey("scheduleExpression") && !json.isNull("scheduleExpression")) {
                builder.scheduleExpression(deserializeScheduleExpression(json.getJsonObject("scheduleExpression")));
            }
            if (json.containsKey("initialDelay")) {
                builder.initialDelay(getLong(json, "initialDelay"));
            }
            if (json.containsKey("afterDelay")) {
                builder.afterDelay(getLong(json, "afterDelay"));
            }
            if (json.containsKey("interval")) {
                builder.interval(getLong(json, "interval"));
            }
            if (json.containsKey("persistent")) {
                builder.persistent(json.getBoolean("persistent"));
            }

            return builder.build();
        }

        private ScheduleExpression deserializeScheduleExpression(JsonObject json) {
            ScheduleExpression expression = new ScheduleExpression();

            setIfPresent(json, "second", expression::second);
            setIfPresent(json, "minute", expression::minute);
            setIfPresent(json, "hour", expression::hour);
            setIfPresent(json, "dayOfMonth", expression::dayOfMonth);
            setIfPresent(json, "dayOfWeek", expression::dayOfWeek);
            setIfPresent(json, "month", expression::month);
            setIfPresent(json, "year", expression::year);
            setIfPresent(json, "timezone", expression::timezone);
            if (json.containsKey("start") && !json.isNull("start")) {
                expression.start(Date.from(Instant.from(DATE_FORMATTER.parse(getAsString(json.get("start"))))));
            }
            if (json.containsKey("end") && !json.isNull("end")) {
                expression.end(Date.from(Instant.from(DATE_FORMATTER.parse(getAsString(json.get("end"))))));
            }

            return expression;
        }

        private void setIfPresent(JsonObject json, String key, Consumer<String> setter) {
            if (json.containsKey(key) && !json.isNull(key)) {
                setter.accept(getAsString(json.get(key)));
            }
        }

        private String getAsString(JsonValue value) {
            if (value.getValueType() == JsonValue.ValueType.STRING) {
                return ((JsonString) value).getString();
            } else if (value.getValueType() == JsonValue.ValueType.NUMBER) {
                return value.toString();
            }
            return value.toString();
        }

        private long getLong(JsonObject json, String key) {
            JsonValue value = json.get(key);
            if (value == null) {
                return 0;
            }
            if (value.getValueType() == JsonValue.ValueType.NUMBER) {
                return ((JsonNumber) value).longValue();
            }
            return Long.parseLong(value.toString());
        }
    }
}
