package com.asc.studycompanion.module.timetable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.auth.entity.UserRole;
import com.asc.studycompanion.module.timetable.controller.TimetableController;
import com.asc.studycompanion.module.timetable.dto.TimetableEntryDto;
import com.asc.studycompanion.module.timetable.dto.TimetableResponse;
import com.asc.studycompanion.module.timetable.dto.UpdateTimetableRequest;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.module.timetable.service.TimetableService;
import com.asc.studycompanion.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TimetableControllerTest {

    @Mock
    private TimetableService timetableService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new TimetableController(timetableService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAndPutTimetableRequireAuthValidateEntriesAndReturnVersionedResponse() throws Exception {
        mockMvc.perform(get("/api/student/timetable"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_001"));

        UserPrincipal student = new UserPrincipal(1L, "s@gmail.com", "h", "Khoi", UserRole.STUDENT);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(student, null, student.getAuthorities())
        );

        when(timetableService.updateTimetable(eq(1L), eq(null), any(UpdateTimetableRequest.class)))
                .thenReturn(new TimetableResponse(
                        10L,
                        2,
                        LocalDate.of(2026, 11, 25),
                        Instant.parse("2026-11-25T03:00:00Z"),
                        "Khoi",
                        List.of(new TimetableEntryDto(1, SubjectCode.MATH, "Tiet 1"))
                ));

        mockMvc.perform(put("/api/student/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entries": [
                                    { "weekday": 8, "subjectCode": "MATH" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VAL_001"));

        mockMvc.perform(put("/api/student/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entries": [
                                    { "weekday": 1, "subjectCode": "MATH", "periodNote": "Tiet 1" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.entries[0].subjectCode").value("MATH"));
    }
}
