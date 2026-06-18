package io.quarkiverse.jberet.runtime;

import static io.quarkiverse.jberet.runtime.QuarkusJobScheduler.toCronExpression;
import static io.quarkiverse.jberet.runtime.QuarkusJobScheduler.toDayOfWeek;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.ejb.ScheduleExpression;

import org.junit.jupiter.api.Test;

class QuarkusJobSchedulerTest {

    @Test
    void wildcards() {
        assertEquals("*", toDayOfWeek("*"));
        assertEquals("?", toDayOfWeek("?"));
        assertNull(toDayOfWeek(null));
    }

    @Test
    void singleNumericValues() {
        assertEquals("1", toDayOfWeek("0"));
        assertEquals("2", toDayOfWeek("1"));
        assertEquals("3", toDayOfWeek("2"));
        assertEquals("4", toDayOfWeek("3"));
        assertEquals("5", toDayOfWeek("4"));
        assertEquals("6", toDayOfWeek("5"));
        assertEquals("7", toDayOfWeek("6"));
        assertEquals("1", toDayOfWeek("7"));
    }

    @Test
    void namedDays() {
        assertEquals("MON", toDayOfWeek("MON"));
        assertEquals("Mon", toDayOfWeek("Mon"));
        assertEquals("FRI", toDayOfWeek("FRI"));
    }

    @Test
    void ranges() {
        assertEquals("2-6", toDayOfWeek("1-5"));
        assertEquals("1-7", toDayOfWeek("0-6"));
        assertEquals("Mon-Fri", toDayOfWeek("Mon-Fri"));
    }

    @Test
    void lists() {
        assertEquals("2,4,6", toDayOfWeek("1,3,5"));
        assertEquals("MON,WED,FRI", toDayOfWeek("MON,WED,FRI"));
    }

    @Test
    void increments() {
        assertEquals("2/2", toDayOfWeek("1/2"));
        assertEquals("1/3", toDayOfWeek("0/3"));
    }

    @Test
    void cronExpressionDefaults() {
        assertEquals("0 0 0 * * ? *", toCronExpression(new ScheduleExpression()));
    }

    @Test
    void cronExpressionWithHour() {
        assertEquals("0 0 6 * * ? *", toCronExpression(new ScheduleExpression().hour(6)));
    }

    @Test
    void cronExpressionWithDayOfWeek() {
        assertEquals("0 0 0 ? * 2 *", toCronExpression(new ScheduleExpression().dayOfWeek(1)));
    }

    @Test
    void cronExpressionWithDayOfMonth() {
        assertEquals("0 0 0 15 * ? *", toCronExpression(new ScheduleExpression().dayOfMonth(15)));
    }
}
