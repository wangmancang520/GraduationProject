package com.example.springboot;

import com.example.springboot.entity.Appointment;
import com.example.springboot.service.AppointmentService;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentServiceTest {
    private Date instant(String value) { return Date.from(Instant.parse(value)); }
    private Appointment booking(Integer id, String start, String end) {
        return new Appointment(id, 1, 1, instant(start), instant(end), 1, 1);
    }
    @Test void combinesYearMonthAndDayInCampusTimezone() throws Exception {
        AppointmentService service = new AppointmentService();
        Appointment result = service.constructor(null, 1, 1, "2030-02-03",
                "2026-12-31T01:00:00.000Z", "2026-12-31T02:00:00.000Z");
        assertEquals(instant("2030-02-03T01:00:00Z"), result.getAStartTime());
        assertEquals(1, result.getAApprovalStatus());
        Appointment offset = service.constructor(null, 1, 1, "2030-02-03", "2030-02-03T09:30:00+08:00", "2030-02-03T10:30:00+08:00");
        assertEquals(instant("2030-02-03T01:30:00Z"), offset.getAStartTime());
    }
    @Test void adjacentIntervalsAreAllowedButOverlapIsRejected() {
        AppointmentService service = spy(new AppointmentService());
        doReturn(Collections.singletonList(booking(256, "2030-01-01T01:00:00Z", "2030-01-01T02:00:00Z")))
                .when(service).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        assertTrue(service.validate(booking(null, "2030-01-01T02:00:00Z", "2030-01-01T03:00:00Z")));
        assertTrue(service.validate(booking(null, "2030-01-01T00:00:00Z", "2030-01-01T01:00:00Z")));
        assertFalse(service.validate(booking(null, "2030-01-01T01:30:00Z", "2030-01-01T03:00:00Z")));
        assertTrue(service.validate(booking(Integer.valueOf("256"), "2030-01-01T01:00:00Z", "2030-01-01T02:00:00Z")));
    }
    @Test void emptyEditTimesReuseTimeOnTheNewDate() throws Exception {
        AppointmentService service = spy(new AppointmentService());
        doReturn(booking(256, "2030-01-01T01:00:00Z", "2030-01-01T02:00:00Z")).when(service).getById(256);
        Appointment result = service.constructor(256, 1, 1, "2031-04-05", new String(""), new String(""));
        assertEquals(instant("2031-04-05T01:00:00Z"), result.getAStartTime());
    }
}
