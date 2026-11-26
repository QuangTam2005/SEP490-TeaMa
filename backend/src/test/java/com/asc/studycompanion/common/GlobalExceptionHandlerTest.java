package com.asc.studycompanion.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.asc.studycompanion.common.dto.ApiResponse;
import com.asc.studycompanion.common.exception.AppException;
import com.asc.studycompanion.common.exception.ErrorCode;
import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    record SamplePayload(
            @NotBlank(message = "Mã không được để trống") String code,
            @NotBlank(message = "Tên không được để trống") String name
    ) {
    }

    @RestController
    @RequestMapping("/test")
    static class DummyTestController {

        @GetMapping("/ok")
        ApiResponse<String> ok() {
            return ApiResponse.ok("OK", "hello");
        }

        @GetMapping("/app-error")
        ApiResponse<Void> appError() {
            throw new AppException(ErrorCode.STUDY_TASK_NOT_FOUND);
        }

        @PostMapping("/validate")
        ApiResponse<String> validate(@Valid @RequestBody SamplePayload payload) {
            return ApiResponse.ok(payload.code());
        }

        @GetMapping("/forbidden")
        ApiResponse<Void> forbidden() {
            throw new AccessDeniedException("Forbidden");
        }

        @GetMapping("/unexpected")
        ApiResponse<Void> unexpected() {
            throw new RuntimeException("Internal sensitive SQL detail");
        }

        @GetMapping("/conflict-unique")
        ApiResponse<Void> conflictUnique() {
            throw new DataIntegrityViolationException("duplicate key uq_timetables_student_version");
        }

        @GetMapping("/conflict-locking")
        ApiResponse<Void> conflictLocking() {
            throw new OptimisticLockingFailureException("stale study_tasks row");
        }
    }

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyTestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void okResponseUsesUnifiedEnvelopeAndIsoTimestamp() throws Exception {
        mockMvc.perform(get("/test/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value("hello"))
                .andExpect(jsonPath("$.errorCode").value(nullValue()))
                .andExpect(jsonPath("$.timestamp").value(matchesPattern("^\\d{4}-\\d{2}-\\d{2}T.*Z$")));
    }

    @Test
    void appExceptionReturnsDomainErrorCodeAndStatus() throws Exception {
        mockMvc.perform(get("/test/app-error"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("STUDY_001"))
                .andExpect(jsonPath("$.message").value(ErrorCode.STUDY_TASK_NOT_FOUND.getDefaultMessage()));
    }

    @Test
    void validationExceptionReturnsFieldErrorsMap() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL_001"))
                .andExpect(jsonPath("$.data.code").value("Mã không được để trống"))
                .andExpect(jsonPath("$.data.name").value("Tên không được để trống"));
    }

    @Test
    void accessDeniedReturns403WithAuthErrorCode() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_004"));
    }

    @Test
    void unhandledExceptionReturns500WithoutLeakingInternalDetails() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SYS_500"))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()));
    }

    @Test
    void integrityAndConcurrencyConflictsReturn409Without500OrSqlLeaks() throws Exception {
        mockMvc.perform(get("/test/conflict-unique"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SYS_409"))
                .andExpect(jsonPath("$.message").value(ErrorCode.CONFLICT.getDefaultMessage()));

        mockMvc.perform(get("/test/conflict-locking"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SYS_409"));

        mockMvc.perform(post("/test/ok"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("SYS_405"));
    }
}
