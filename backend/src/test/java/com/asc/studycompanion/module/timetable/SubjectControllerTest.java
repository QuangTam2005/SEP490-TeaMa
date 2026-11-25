package com.asc.studycompanion.module.timetable;

import com.asc.studycompanion.common.exception.GlobalExceptionHandler;
import com.asc.studycompanion.module.timetable.controller.SubjectController;
import com.asc.studycompanion.module.timetable.entity.Subject;
import com.asc.studycompanion.module.timetable.entity.SubjectCode;
import com.asc.studycompanion.module.timetable.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubjectControllerTest {

    @Mock
    private SubjectRepository subjectRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SubjectController(subjectRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsAllFiveActiveGrade6Subjects() throws Exception {
        when(subjectRepository.findAllByIsActiveTrueOrderByIdAsc()).thenReturn(List.of(
                Subject.builder().id(1L).code(SubjectCode.MATH).name("Toán").isActive(true).build(),
                Subject.builder().id(2L).code(SubjectCode.LIT).name("Ngữ văn").isActive(true).build(),
                Subject.builder().id(3L).code(SubjectCode.ENG).name("Tiếng Anh").isActive(true).build(),
                Subject.builder().id(4L).code(SubjectCode.SCI).name("Khoa học tự nhiên").isActive(true).build(),
                Subject.builder().id(5L).code(SubjectCode.HISGEO).name("Lịch sử – Địa lý").isActive(true).build()
        ));

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].code").value("MATH"))
                .andExpect(jsonPath("$.data[4].code").value("HISGEO"));
    }
}
